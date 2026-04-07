import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1200'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const queries = [
  'pani puri',
  'khaman',
  'dabeli in navarangpura',
  'sandwich',
  'items in ahmedabad',
  'cheese',
  'best pani puri in ahmedabad',
];

export default function () {
  const query = queries[Math.floor(Math.random() * queries.length)];
  const url = `${BASE_URL}/search/items/smart?query=${encodeURIComponent(query)}`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}