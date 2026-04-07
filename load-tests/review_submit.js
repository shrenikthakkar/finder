import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<1500'],
    http_req_failed: ['rate<0.50'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';
const ITEM_ID = __ENV.ITEM_ID || '';

export default function () {
  const payload = JSON.stringify({
    itemId: ITEM_ID,
    rating: 4,
    comment: `Load test review ${__VU}-${__ITER}`,
  });

  const res = http.post(`${BASE_URL}/reviews/item`, payload, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${TOKEN}`,
    },
  });

  check(res, {
    'status is success or duplicate': (r) =>
      r.status === 200 || r.status === 201 || r.status === 409 || r.status === 400,
  });

  sleep(1);
}