/* eslint-disable no-console */
const fs = require('fs');
const path = require('path');
const selfsigned = require('selfsigned');

const sslDir = path.resolve(__dirname, '../src/main/resources/localhost-ssl');
const keyPath = path.join(sslDir, 'localhost.key');
const certPath = path.join(sslDir, 'localhost.crt');

if (!fs.existsSync(sslDir)) fs.mkdirSync(sslDir, { recursive: true });

if (fs.existsSync(keyPath) && fs.existsSync(certPath)) {
    console.log('[dev] Certs already present, skipping generation.');
    process.exit(0);
}

console.log('[dev] Creating self-signed certs for localhost…');

const attrs = [{ name: 'commonName', value: 'localhost' }];
const pems = selfsigned.generate(attrs, {
    days: 3650,
    keySize: 2048,
    algorithm: 'sha256',
    extensions: [
        {
            name: 'subjectAltName',
            altNames: [
                { type: 2, value: 'localhost' },        // DNS
                { type: 7, ip: '127.0.0.1' },           // IPv4
                { type: 7, ip: '::1' }                  // IPv6
            ]
        }
    ]
});

fs.writeFileSync(keyPath, pems.private, { encoding: 'utf8' });
fs.writeFileSync(certPath, pems.cert, { encoding: 'utf8' });

console.log('[dev] Wrote:');
console.log('  ', keyPath);
console.log('  ', certPath);
