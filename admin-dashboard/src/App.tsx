import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { auth, AuthUser } from './api/client';
import Layout from './components/Layout';
import { Toaster } from './components/ui';
import Login from './pages/Login';
import DashboardPage from './pages/Dashboard';
import Orders from './pages/Orders';
import OrderDetailPage from './pages/OrderDetail';
import InvoicePage from './pages/Invoice';
import Products from './pages/Products';
import ProductForm from './pages/ProductForm';
import Categories from './pages/Categories';
import Inventory from './pages/Inventory';
import ImportPage from './pages/Import';
import Brands from './pages/Brands';
import Customers from './pages/Customers';
import DeliveryPartners from './pages/DeliveryPartners';
import Coupons from './pages/Coupons';
import Banners from './pages/Banners';
import Pincodes from './pages/Pincodes';
import Reports from './pages/Reports';
import Refunds from './pages/Refunds';
import Reviews from './pages/Reviews';
import Support from './pages/Support';
import Notifications from './pages/Notifications';
import Settings from './pages/Settings';
import Audit from './pages/Audit';

export default function App() {
  const [user, setUser] = useState<AuthUser | null>(auth.user());
  useEffect(() => auth.subscribe(setUser), []);
  return (
    <BrowserRouter>
      <Toaster />
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
        <Route path="/orders/:id/invoice" element={user ? <InvoicePage /> : <Navigate to="/login" replace />} />
        <Route element={user ? <Layout /> : <Navigate to="/login" replace />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/orders/:id" element={<OrderDetailPage />} />
          <Route path="/products" element={<Products />} />
          <Route path="/products/new" element={<ProductForm />} />
          <Route path="/products/:id" element={<ProductForm />} />
          <Route path="/categories" element={<Categories />} />
          <Route path="/inventory" element={<Inventory />} />
          <Route path="/import" element={<ImportPage />} />
          <Route path="/brands" element={<Brands />} />
          <Route path="/customers" element={<Customers />} />
          <Route path="/delivery-partners" element={<DeliveryPartners />} />
          <Route path="/coupons" element={<Coupons />} />
          <Route path="/banners" element={<Banners />} />
          <Route path="/pincodes" element={<Pincodes />} />
          <Route path="/reports" element={<Reports />} />
          <Route path="/refunds" element={<Refunds />} />
          <Route path="/reviews" element={<Reviews />} />
          <Route path="/support" element={<Support />} />
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/audit" element={<Audit />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
