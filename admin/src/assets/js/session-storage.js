const SESSION_ALL_TRAIN = 'SESSION_ALL_TRAIN'

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

Object.assign(window, {
  SESSION_ALL_TRAIN,
  SessionStorage,
})
