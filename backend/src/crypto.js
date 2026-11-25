'use strict';

const crypto = require('node:crypto');

const BASE32_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

function canonicalJson(value) {
  if (value === null) return 'null';
  if (value === undefined) return undefined;
  if (typeof value === 'string') return JSON.stringify(value);
  if (typeof value === 'boolean') return value ? 'true' : 'false';
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) throw new Error('Non-finite number is not canonical');
    return String(value);
  }
  if (Array.isArray(value)) return `[${value.map((item) => canonicalJson(item) ?? 'null').join(',')}]`;
  if (typeof value === 'object') {
    const keys = Object.keys(value)
      .filter((key) => value[key] !== undefined)
      .sort();
    return `{${keys.map((key) => `${canonicalJson(key)}:${canonicalJson(value[key])}`).join(',')}}`;
  }
  throw new Error(`Unsupported canonical JSON value: ${typeof value}`);
}

function sha256(input) {
  return crypto.createHash('sha256').update(input).digest();
}

function sha256Hex(input) {
  return sha256(input).toString('hex');
}

function base32(bytes) {
  let bits = 0;
  let value = 0;
  let output = '';
  for (const byte of bytes) {
    value = (value << 8) | byte;
    bits += 8;
    while (bits >= 5) {
      output += BASE32_ALPHABET[(value >>> (bits - 5)) & 31];
      bits -= 5;
    }
  }
  if (bits > 0) output += BASE32_ALPHABET[(value << (5 - bits)) & 31];
  return output;
}

function randomToken(bytes = 32) {
  return crypto.randomBytes(bytes).toString('base64url');
}

function randomDigits(count) {
  let output = '';
  while (output.length < count) {
    const byte = crypto.randomBytes(1)[0];
    if (byte >= 250) continue;
    output += String(byte % 10);
  }
  return output;
}

function publicKeyFromDer(derBase64) {
  return crypto.createPublicKey({
    key: Buffer.from(derBase64, 'base64'),
    format: 'der',
    type: 'spki'
  });
}

function publicFingerprint(derBase64) {
  return sha256(Buffer.from(derBase64, 'base64'))
    .toString('hex')
    .toUpperCase()
    .replace(/(.{2})(?=.)/g, '$1:');
}

function safetyCode(idA, fingerprintA, idB, fingerprintB) {
  const pair = [
    { id: idA, fingerprint: fingerprintA },
    { id: idB, fingerprint: fingerprintB }
  ].sort((a, b) => {
    const left = `${a.id}|${a.fingerprint}`;
    const right = `${b.id}|${b.fingerprint}`;
    return left < right ? -1 : left > right ? 1 : 0;
  });
  const digest = sha256(`${pair[0].id}:${pair[0].fingerprint}|${pair[1].id}:${pair[1].fingerprint}`);
  const code = base32(digest).slice(0, 20);
  return code.replace(/(.{4})(?=.)/g, '$1 ');
}

function createSignature(unsigned, privateKey) {
  return crypto.sign('sha256', Buffer.from(canonicalJson(unsigned)), privateKey).toString('base64');
}

function verifySignature(unsigned, signatureBase64, publicKeyDer) {
  try {
    return crypto.verify(
      'sha256',
      Buffer.from(canonicalJson(unsigned)),
      publicKeyFromDer(publicKeyDer),
      Buffer.from(signatureBase64, 'base64')
    );
  } catch {
    return false;
  }
}

function constantTimeEqual(left, right) {
  const a = crypto.createHash('sha256').update(String(left || '')).digest();
  const b = crypto.createHash('sha256').update(String(right || '')).digest();
  return crypto.timingSafeEqual(a, b);
}

function turnCredentials(secret, userId, ttlSeconds) {
  const expires = Math.floor(Date.now() / 1000) + ttlSeconds;
  const username = `${expires}:${userId}`;
  const password = crypto.createHmac('sha1', secret).update(username).digest('base64');
  return { username, credential: password, expires };
}

module.exports = {
  canonicalJson,
  sha256,
  sha256Hex,
  base32,
  randomToken,
  randomDigits,
  publicKeyFromDer,
  publicFingerprint,
  safetyCode,
  createSignature,
  verifySignature,
  constantTimeEqual,
  turnCredentials
};
