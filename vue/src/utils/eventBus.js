const listeners = new Map()

export const eventBus = {
  $on(event, handler) {
    if (!listeners.has(event)) {
      listeners.set(event, new Set())
    }
    listeners.get(event).add(handler)
  },

  $off(event, handler) {
    if (!listeners.has(event)) {
      return
    }

    if (!handler) {
      listeners.delete(event)
      return
    }

    const handlers = listeners.get(event)
    handlers.delete(handler)
    if (handlers.size === 0) {
      listeners.delete(event)
    }
  },

  $emit(event, ...args) {
    if (!listeners.has(event)) {
      return
    }

    listeners.get(event).forEach(handler => {
      handler(...args)
    })
  }
}

export const EVENTS = {
  IRRIGATION_ON: 'irrigation:on',
  IRRIGATION_OFF: 'irrigation:off',
  LED_ON: 'led:on',
  LED_OFF: 'led:off'
}
