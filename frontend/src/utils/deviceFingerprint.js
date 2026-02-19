/**
 * Device fingerprint input for backend SHA-256 hashing.
 * Collects: User-Agent, Screen Resolution, Timezone, Language (non-sensitive).
 * Backend concatenates and hashes to detect Device Spoofing.
 */
const DEVICE_ID_KEY = 'upi_device_id';

export function getDeviceId() {
  let id = localStorage.getItem(DEVICE_ID_KEY);
  if (!id) {
    id = crypto.randomUUID ? crypto.randomUUID() : `dev-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    localStorage.setItem(DEVICE_ID_KEY, id);
  }
  return id;
}

export function getDeviceFingerprintInput() {
  const ua = typeof navigator !== 'undefined' ? navigator.userAgent : '';
  const screenStr =
    typeof window !== 'undefined' && window.screen
      ? `${window.screen.width}x${window.screen.height}`
      : '0x0';
  const timezone = typeof Intl !== 'undefined' && Intl.DateTimeFormat
    ? Intl.DateTimeFormat().resolvedOptions().timeZone
    : '';
  const language = typeof navigator !== 'undefined'
    ? (navigator.language || navigator.userLanguage || '')
    : '';
  return [ua, screenStr, timezone, language].join('|');
}
