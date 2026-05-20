const Tool = {
  isEmpty(obj) {
    if (typeof obj === 'string') {
      return !obj || obj.replace(/\s+/g, '') === ''
    }
    return !obj || JSON.stringify(obj) === '{}' || obj.length === 0
  },

  isNotEmpty(obj) {
    return !Tool.isEmpty(obj)
  },

  copy(obj) {
    if (Tool.isNotEmpty(obj)) {
      return JSON.parse(JSON.stringify(obj))
    }
    return obj
  },

  array2Tree(array, parentId) {
    if (Tool.isEmpty(array)) {
      return []
    }

    const result = []
    for (let i = 0; i < array.length; i++) {
      const item = array[i]
      if (Number(item.parent) === Number(parentId)) {
        result.push(item)

        const children = Tool.array2Tree(array, item.id)
        if (Tool.isNotEmpty(children)) {
          item.children = children
        }
      }
    }
    return result
  },

  uuid(len, radix = 62) {
    const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split('')
    const uuid = []

    for (let i = 0; i < len; i++) {
      uuid[i] = chars[0 | Math.random() * radix]
    }

    return uuid.join('')
  },
}

window.Tool = Tool

export default Tool
