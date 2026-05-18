export const SESSION_ORDER = 'SESSION_ORDER'
export const SESSION_TICKET_PARAMS = 'SESSION_TICKET_PARAMS'

const SessionStorage = {
  get(key) {
    const value = sessionStorage.getItem(key)
    if (value && value !== 'undefined') {
      return JSON.parse(value)
    }
    return null
  },

  set(key, data) {
    sessionStorage.setItem(key, JSON.stringify(data))
  },

  remove(key) {
    sessionStorage.removeItem(key)
  },

  clearAll() {
    sessionStorage.clear()
  },
}

export default SessionStorage
