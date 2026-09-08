import { useState, useEffect } from 'react';
import { upi } from '../api/client';
import { getDeviceId, getDeviceFingerprintInput } from '../utils/deviceFingerprint';
import styles from './Dashboard.module.css';

export default function Dashboard() {
  const [balance, setBalance] = useState(null);
  const [history, setHistory] = useState([]);
  const [receiverUpiId, setReceiverUpiId] = useState('');
  const [amount, setAmount] = useState('');
  const [location, setLocation] = useState({ lat: null, lon: null });
  const [loading, setLoading] = useState(false);
  const [payResult, setPayResult] = useState(null);
  const [error, setError] = useState('');

  const loadBalance = async () => {
    try {
      const res = await upi.balance();
      setBalance(res.balance);
    } catch (e) {
      setBalance(0);
    }
  };

  const loadHistory = async () => {
    try {
      const list = await upi.history(0, 15);
      setHistory(Array.isArray(list) ? list : []);
    } catch {
      setHistory([]);
    }
  };

  useEffect(() => {
    loadBalance();
    loadHistory();
  }, []);

  useEffect(() => {
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (p) => setLocation({ lat: p.coords.latitude, lon: p.coords.longitude }),
        () => {},
        { enableHighAccuracy: false, timeout: 5000, maximumAge: 300000 }
      );
    }
  }, []);

  const handlePay = async (e) => {
    e.preventDefault();
    setError('');
    setPayResult(null);
    setLoading(true);
    try {
      // Generate unique idempotency key to prevent accidental double debiting on retries
      const idempotencyKey = typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

      const body = {
        receiverUpiId: receiverUpiId.trim(),
        amount: parseFloat(amount),
        deviceId: getDeviceId(),
        deviceFingerprintInput: getDeviceFingerprintInput(),
        latitude: location.lat ?? undefined,
        longitude: location.lon ?? undefined,
        idempotencyKey,
      };
      const res = await upi.pay(body);
      setPayResult(res);
      setReceiverUpiId('');
      setAmount('');
      loadBalance();
      loadHistory();
    } catch (err) {
      setError(err.body?.message || err.body || err.message || 'Payment failed');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (d) => {
    if (!d) return '-';
    const date = new Date(d);
    return date.toLocaleString();
  };

  const statusClass = (s) => {
    if (s === 'SUCCESS') return styles.success;
    if (s === 'BLOCKED' || s === 'FAILED') return styles.danger;
    if (s === 'SUSPICIOUS') return styles.warning;
    return '';
  };

  return (
    <div className={styles.dashboard}>
      <h1 className={styles.heading}>Dashboard</h1>

      <div className={styles.balanceCard}>
        <span className={styles.balanceLabel}>Available balance</span>
        <span className={styles.balanceValue}>
          {balance != null ? `Rs ${Number(balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '...'}
        </span>
      </div>

      <section className={styles.section}>
        <h2>Send money</h2>
        <form onSubmit={handlePay} className={styles.form}>
          {error && <div className={styles.error}>{error}</div>}
          {payResult && (
            <div className={`${styles.result} ${statusClass(payResult.status)}`}>
              {payResult.message}
              {payResult.riskScore != null && payResult.riskScore > 0 && (
                <span> Risk score: {payResult.riskScore} ({payResult.riskLevel})</span>
              )}
            </div>
          )}
          <label>
            Receiver UPI ID
            <input
              type="text"
              value={receiverUpiId}
              onChange={(e) => setReceiverUpiId(e.target.value)}
              placeholder="receiver@paytm"
              required
            />
          </label>
          <label>
            Amount (Rs)
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </label>
          <p className={styles.hint}>
            Device fingerprint (UA, screen, timezone, language) and optional location are sent for fraud checks.
          </p>
          <button type="submit" disabled={loading} className={styles.submit}>
            {loading ? 'Processing...' : 'Send'}
          </button>
        </form>
      </section>

      <section className={styles.section}>
        <h2>Recent transactions</h2>
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Time</th>
                <th>To</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Risk</th>
              </tr>
            </thead>
            <tbody>
              {history.length === 0 && (
                <tr><td colSpan={5} className={styles.empty}>No transactions yet</td></tr>
              )}
              {history.map((tx) => (
                <tr key={tx.transactionId}>
                  <td>{formatDate(tx.createdAt)}</td>
                  <td className={styles.mono}>{tx.receiverUpiId}</td>
                  <td>Rs {Number(tx.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                  <td><span className={statusClass(tx.status)}>{tx.status}</span></td>
                  <td>{tx.riskScore != null ? tx.riskScore : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
