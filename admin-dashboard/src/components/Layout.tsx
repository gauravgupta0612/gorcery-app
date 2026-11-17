import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { auth } from '../api/client';

const NAV: { to: string; label: string; icon: string; adminOnly?: boolean }[] = [
  { to: '/', label: 'Dashboard', icon: '📊' },
  { to: '/orders', label: 'Orders', icon: '🧾' },
  { to: '/products', label: 'Products', icon: '🥦' },
  { to: '/categories', label: 'Categories', icon: '🗂️' },
  { to: '/inventory', label: 'Inventory', icon: '📦' },
  { to: '/import', label: 'Bulk import', icon: '⬆️' },
  { to: '/brands', label: 'Brands & suppliers', icon: '🏷️' },
  { to: '/customers', label: 'Customers', icon: '👥' },
  { to: '/delivery-partners', label: 'Delivery partners', icon: '🛵' },
  { to: '/coupons', label: 'Coupons & offers', icon: '🎟️' },
  { to: '/banners', label: 'Banners', icon: '🖼️' },
  { to: '/pincodes', label: 'Pincodes & charges', icon: '📍' },
  { to: '/reports', label: 'Sales reports', icon: '📈' },
  { to: '/refunds', label: 'Refunds', icon: '↩️' },
  { to: '/reviews', label: 'Reviews', icon: '⭐' },
  { to: '/support', label: 'Support', icon: '💬' },
  { to: '/notifications', label: 'Notifications', icon: '🔔', adminOnly: true },
  { to: '/settings', label: 'Store settings', icon: '⚙️' },
  { to: '/audit', label: 'Audit log', icon: '🛡️', adminOnly: true },
];

export default function Layout() {
  const nav = useNavigate();
  const user = auth.user();
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <img src="/logo.svg" alt="" width={36} height={36} />
          <div><strong>Gola Grocery</strong><small>Admin console</small></div>
        </div>
        <nav>
          {NAV.filter((n) => !n.adminOnly || auth.isAdmin()).map((n) => (
            <NavLink key={n.to} to={n.to} end={n.to === '/'} className={({ isActive }) => (isActive ? 'active' : '')}>
              <span aria-hidden>{n.icon}</span>{n.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="main">
        <header className="topbar">
          <span className="tagline">Freshness Delivered to Your Doorstep.</span>
          <div className="who">
            <span>{user?.fullName} <small>({user?.roles.filter((r) => r !== 'CUSTOMER').join(', ')})</small></span>
            <button className="btn" onClick={async () => { await auth.logout(); nav('/login'); }}>Log out</button>
          </div>
        </header>
        <main className="content"><Outlet /></main>
      </div>
    </div>
  );
}
