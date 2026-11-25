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

  const saveSession = useCallback(({ token, account }) => {
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(account))
    setUser(account)
    return account
  }, [])

  const login = async (email, password) => saveSession((await api.post('/api/v1/auth/login', { email, password })).data)
  /** body: { email, password, fullName, phone } */
  const register = async (body) => saveSession((await api.post('/api/v1/auth/register', body)).data)
  const loginGoogle = async (credential) => saveSession((await api.post('/api/v1/auth/google', { credential })).data)

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
    <AuthContext.Provider value={{ user, login, register, loginGoogle, logout, updateUser, isAdmin: user?.role === 'ADMIN' }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext)
