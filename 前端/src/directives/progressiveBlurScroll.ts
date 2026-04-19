import type { ObjectDirective } from 'vue'

type BlurScrollElement = HTMLElement & {
  __pbCleanup__?: () => void
}

const TOP_ATTR = 'data-pb-top'
const BOTTOM_ATTR = 'data-pb-bottom'

const updateState = (el: HTMLElement) => {
  const canScroll = el.scrollHeight - el.clientHeight > 2
  const showTop = canScroll && el.scrollTop > 2
  const showBottom = canScroll && el.scrollTop + el.clientHeight < el.scrollHeight - 2

  el.setAttribute(TOP_ATTR, showTop ? '1' : '0')
  el.setAttribute(BOTTOM_ATTR, showBottom ? '1' : '0')
}

const ensureEdge = (className: string) => {
  const edge = document.createElement('div')
  edge.className = className
  edge.setAttribute('aria-hidden', 'true')
  return edge
}

export const progressiveBlurScrollDirective: ObjectDirective<BlurScrollElement> = {
  mounted(el) {
    el.classList.add('progressive-blur-scroll')

    const topEdge = ensureEdge('progressive-blur-scroll__edge progressive-blur-scroll__edge--top')
    const bottomEdge = ensureEdge('progressive-blur-scroll__edge progressive-blur-scroll__edge--bottom')

    el.prepend(topEdge)
    el.append(bottomEdge)

    const sync = () => updateState(el)

    el.addEventListener('scroll', sync, { passive: true })
    window.addEventListener('resize', sync)
    requestAnimationFrame(sync)

    el.__pbCleanup__ = () => {
      el.removeEventListener('scroll', sync)
      window.removeEventListener('resize', sync)
      topEdge.remove()
      bottomEdge.remove()
      el.classList.remove('progressive-blur-scroll')
      el.removeAttribute(TOP_ATTR)
      el.removeAttribute(BOTTOM_ATTR)
    }
  },

  updated(el) {
    requestAnimationFrame(() => updateState(el))
  },

  unmounted(el) {
    el.__pbCleanup__?.()
    delete el.__pbCleanup__
  },
}

