import { useState } from 'react';
import { api } from '../api/client';
import { AuditEntry, Page } from '../api/types';
import { Loadable, Pager, useAsync } from '../components/ui';
import { dateTime } from '../utils/format';

export default function Audit() {
  const [page, setPage] = useState(0);
  const state = useAsync(() => api.get<Page<AuditEntry>>('/api/v1/admin/audit-logs', { page }), [page]);
  return (
    <>
      <div className="page-head"><h1>Audit log</h1></div>
      <Loadable state={state}>{(d) => (
        <>
          <div className="card flush"><table className="table compact">
            <thead><tr><th>When</th><th>Actor</th><th>Action</th><th>Entity</th><th>Details</th><th>IP</th></tr></thead>
            <tbody>{d.items.map((a) => (
              <tr key={a.id}><td><small>{dateTime(a.createdAt)}</small></td><td>{a.actorId ? `#${a.actorId}` : 'system'} <small className="muted">{a.actorRole}</small></td>
                <td><code>{a.action}</code></td><td>{a.entityType} {a.entityId}</td><td><small>{a.details}</small></td><td><small>{a.ipAddress}</small></td></tr>
            ))}</tbody></table></div>
          <Pager page={d.page} totalPages={d.totalPages} onPage={setPage} />
        </>
      )}</Loadable>
    </>
  );
}
