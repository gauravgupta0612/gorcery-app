import { useState } from 'react';
import { api } from '../api/client';
import { Page } from '../api/types';
import { Badge, Empty, Loadable, Pager, toast, useAsync } from '../components/ui';
import { dateTime } from '../utils/format';

interface ReviewRow { id: number; productId: number; productName: string; userId: number; rating: number; title: string; comment: string; approved: boolean; createdAt: string }

export default function Reviews() {
  const [page, setPage] = useState(0);
  const state = useAsync(() => api.get<Page<ReviewRow>>('/api/v1/admin/reviews', { page }), [page]);
  async function toggle(r: ReviewRow) {
    try { await api.patch(`/api/v1/admin/reviews/${r.id}`, { approved: !r.approved }); toast.ok(r.approved ? 'Review hidden' : 'Review published'); state.reload(); } catch (e) { toast.err(e); }
  }
  return (
    <>
      <div className="page-head"><h1>Ratings & reviews</h1></div>
      <Loadable state={state} empty={(d) => (d.items.length === 0 ? <Empty title="No reviews yet" hint="Customers can review products from delivered orders." /> : null)}>{(d) => (
        <>
          <div className="card flush"><table className="table">
            <thead><tr><th>Product</th><th>Rating</th><th>Review</th><th>Date</th><th>Status</th><th></th></tr></thead>
            <tbody>{d.items.map((r) => (
              <tr key={r.id}><td>{r.productName}</td><td>{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</td><td><strong>{r.title}</strong><br />{r.comment}</td>
                <td><small>{dateTime(r.createdAt)}</small></td><td><Badge value={r.approved ? 'PUBLISHED' : 'HIDDEN'} tone={r.approved ? 'green' : 'grey'} /></td>
                <td><button className="btn small" onClick={() => toggle(r)}>{r.approved ? 'Hide' : 'Publish'}</button></td></tr>
            ))}</tbody></table></div>
          <Pager page={d.page} totalPages={d.totalPages} onPage={setPage} />
        </>
      )}</Loadable>
    </>
  );
}
