import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import PublicLayout from './layouts/PublicLayout.jsx'
import AdminLayout from './layouts/AdminLayout.jsx'

import Home from './pages/public/Home.jsx'
import About from './pages/public/About.jsx'
import Services from './pages/public/Services.jsx'
import TourDetail from './pages/public/TourDetail.jsx'
import Booking from './pages/public/Booking.jsx'
import BookingView from './pages/public/BookingView.jsx'
import Lookup from './pages/public/Lookup.jsx'
import PaymentResult from './pages/public/PaymentResult.jsx'
import News from './pages/public/News.jsx'
import NewsDetail from './pages/public/NewsDetail.jsx'
import Contact from './pages/public/Contact.jsx'
import Login from './pages/public/Login.jsx'
import Register from './pages/public/Register.jsx'
import Account from './pages/public/Account.jsx'

import Dashboard from './pages/admin/Dashboard.jsx'
import AdminTours from './pages/admin/Tours.jsx'
import TourForm from './pages/admin/TourForm.jsx'
import AdminBookings from './pages/admin/Bookings.jsx'
import AdminBookingDetail from './pages/admin/BookingDetail.jsx'
import CancelRequests from './pages/admin/CancelRequests.jsx'
import AdminNews from './pages/admin/News.jsx'
import Contacts from './pages/admin/Contacts.jsx'
import Catalogs from './pages/admin/Catalogs.jsx'
import PageConfig from './pages/admin/PageConfig.jsx'
import Users from './pages/admin/Users.jsx'

function RequireAdmin({ children }) {
  const { user, isAdmin } = useAuth()
  if (!user) return <Navigate to="/dang-nhap?next=/admin" replace />
  if (!isAdmin) return <Navigate to="/" replace />
  return children
}

function RequireLogin({ children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to="/dang-nhap?next=/tai-khoan" replace />
}

export default function App() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route index element={<Home />} />
        <Route path="gioi-thieu" element={<About />} />
        <Route path="dich-vu" element={<Services />} />
        <Route path="tour/:id" element={<TourDetail />} />
        <Route path="dat-tour/:dotId" element={<Booking />} />
        <Route path="don/:maDon" element={<BookingView />} />
        <Route path="tra-cuu" element={<Lookup />} />
        <Route path="thanh-toan/ket-qua" element={<PaymentResult />} />
        <Route path="tin-tuc" element={<News />} />
        <Route path="tin-tuc/:slug" element={<NewsDetail />} />
        <Route path="lien-he" element={<Contact />} />
        <Route path="dang-nhap" element={<Login />} />
        <Route path="dang-ky" element={<Register />} />
        <Route path="tai-khoan" element={<RequireLogin><Account /></RequireLogin>} />
      </Route>

      <Route path="admin" element={<RequireAdmin><AdminLayout /></RequireAdmin>}>
        <Route index element={<Dashboard />} />
        <Route path="tours" element={<AdminTours />} />
        <Route path="tours/moi" element={<TourForm />} />
        <Route path="tours/:id" element={<TourForm />} />
        <Route path="don-dat" element={<AdminBookings />} />
        <Route path="don-dat/:id" element={<AdminBookingDetail />} />
        <Route path="yeu-cau-huy" element={<CancelRequests />} />
        <Route path="tin-tuc" element={<AdminNews />} />
        <Route path="lien-he" element={<Contacts />} />
        <Route path="danh-muc" element={<Catalogs />} />
        <Route path="gioi-thieu" element={<PageConfig />} />
        <Route path="nguoi-dung" element={<Users />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
