import { createContext, useContext, useState, useCallback } from 'react'
import api from './api'

const AuthContext = createContext(null)

const readUser = () => {
  try {
    return JSON.parse(localStorage.getItem('user'))
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser)

  const saveSession = useCallback(({ token, user }) => {
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    setUser(user)
    return user
  }, [])

  const login = async (email, matKhau) => saveSession((await api.post('/api/auth/login', { email, matKhau })).data)
  const register = async (body) => saveSession((await api.post('/api/auth/register', body)).data)
  const loginGoogle = async (credential) => saveSession((await api.post('/api/auth/google', { credential })).data)

  const updateUser = (u) => {
    localStorage.setItem('user', JSON.stringify(u))
    setUser(u)
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, register, loginGoogle, logout, updateUser, isAdmin: user?.vaiTro === 'ADMIN' }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext)
