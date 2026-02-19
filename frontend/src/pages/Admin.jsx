import { useState, useEffect } from 'react';
import { admin } from '../api/client';
import styles from './Admin.module.css';

export default function Admin() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [userId, setUserId] = useState('');

  const loadLogs = async () => {
    setLoading(true);
    setError('');
    const uid = userId.trim() === '' ? null : parseInt(userId, 10);
    const validUid = uid != null && !Number.isNaN(uid) ? uid : null;
    try {
      const list = await admin.fraudLogs(validUid, 0, 100);
      setLogs(Array.isArray(list) ? list : []);
    } catch (e) {
      setError(e.message || 'Failed to load fraud logs');
      setLogs([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, []);

  const formatDate = (d) => {
    if (!d) return '-';
    return new Date(d).toLocaleString();
  };

  return (
    <div className={styles.admin}>
      <h1 className={styles.heading}>Admin – Fraud monitoring</h1>
      <p className={styles.subtitle}>Flagged transactions and risk events</p>

      <div className={styles.toolbar}>
        <input
          type="number"
          placeholder="Filter by user ID"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          className={styles.input}
        />
        <button type="button" onClick={loadLogs} disabled={loading} className={styles.btn}>
          {loading ? 'Loading...' : 'Refresh'}
        </button>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>ID</th>
              <th>Transaction ID</th>
              <th>User ID</th>
              <th>Reason</th>
              <th>Risk points</th>
              <th>Total score</th>
              <th>Flagged at</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 && !loading && (
              <tr><td colSpan={7} className={styles.empty}>No fraud logs found</td></tr>
            )}
            {logs.map((log) => (
              <tr key={log.id}>
                <td>{log.id}</td>
                <td>{log.transactionId}</td>
                <td>{log.userId}</td>
                <td>{log.fraudReason}</td>
                <td>{log.riskPoints}</td>
                <td>{log.totalRiskScore}</td>
                <td>{formatDate(log.flaggedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
