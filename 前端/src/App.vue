<template>
  <PageLoader :active="pageLoading" :message="loadingMessage" />
  <BackgroundParticles />
  <TargetCursor :spin-duration="0" hide-default-cursor :hover-duration="0.12" />

  <RouterView v-slot="{ Component, route }">
    <Transition name="route-shell" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </Transition>
  </RouterView>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import BackgroundParticles from './components/BackgroundParticles.vue'
import PageLoader from './components/PageLoader.vue'
import TargetCursor from './components/TargetCursor.vue'

const router = useRouter()
const pageLoading = ref(false)
const loadingMessage = ref('Loading')

let pendingTimer = null
let clearBeforeEach = null
let clearBeforeResolve = null
let clearAfterEach = null
let clearOnError = null

const startLoading = (to) => {
  window.clearTimeout(pendingTimer)
  loadingMessage.value = 'Loading'
  pendingTimer = window.setTimeout(() => {
    pageLoading.value = true
  }, 180)
}

const stopLoading = () => {
  window.clearTimeout(pendingTimer)
  pendingTimer = null
  window.setTimeout(() => {
    pageLoading.value = false
  }, 160)
}

watch(pageLoading, (value) => {
  document.body.classList.toggle('is-page-loading', value)
})

onMounted(() => {
  clearBeforeEach = router.beforeEach((to, from, next) => {
    if (to.fullPath !== from.fullPath) {
      startLoading(to)
    }
    next()
  })

  clearBeforeResolve = router.beforeResolve((to, from, next) => {
    if (to.fullPath === from.fullPath) {
      stopLoading()
    }
    next()
  })

  clearAfterEach = router.afterEach(() => {
    stopLoading()
  })

  clearOnError = router.onError(() => {
    stopLoading()
  })
})

onBeforeUnmount(() => {
  window.clearTimeout(pendingTimer)
  document.body.classList.remove('is-page-loading')
  clearBeforeEach?.()
  clearBeforeResolve?.()
  clearAfterEach?.()
  clearOnError?.()
})
</script>

<style scoped>
:global(body.is-page-loading) {
  overflow: hidden;
}

:global(.route-shell-enter-active),
:global(.route-shell-leave-active) {
  will-change: opacity, transform, filter;
  transition:
    opacity 0.42s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.46s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.46s cubic-bezier(0.22, 1, 0.36, 1);
}

:global(.route-shell-enter-from),
:global(.route-shell-leave-to) {
  opacity: 0;
  transform: translateY(18px) scale(0.985);
  filter: blur(10px);
}

:global(.route-shell-enter-to),
:global(.route-shell-leave-from) {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
}
</style>
