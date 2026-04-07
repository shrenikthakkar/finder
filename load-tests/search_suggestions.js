import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<800'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const queries = ['pan', 'kham', 'dabel', 'puri', 'sand', 'chee'];

export default function () {
  const query = queries[Math.floor(Math.random() * queries.length)];
  const res = http.get(`${BASE_URL}/search/suggestions?query=${encodeURIComponent(query)}`);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}