<template>
  <canvas ref="canvasRef" class="background-particles" aria-hidden="true"></canvas>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvasRef = ref(null)

let ctx = null
let rafId = 0
let resizeObserver = null
let particles = []
let width = 0
let height = 0

const pointer = {
  x: -9999,
  y: -9999,
  active: false,
}

const createParticle = () => ({
  x: Math.random() * width,
  y: Math.random() * height,
  vx: (Math.random() - 0.5) * 0.42,
  vy: (Math.random() - 0.5) * 0.42,
  size: 0.9 + Math.random() * 1.8,
})

const getParticleCount = () => {
  const density = Math.min(width * height / 18000, 90)
  return Math.max(32, Math.round(density))
}

const resetCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const dpr = window.devicePixelRatio || 1
  width = window.innerWidth
  height = window.innerHeight
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`

  ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  particles = Array.from({ length: getParticleCount() }, createParticle)
}

const drawConnections = () => {
  if (!ctx) return

  for (let i = 0; i < particles.length; i += 1) {
    const a = particles[i]
    for (let j = i + 1; j < particles.length; j += 1) {
      const b = particles[j]
      const dx = a.x - b.x
      const dy = a.y - b.y
      const distance = Math.hypot(dx, dy)

      if (distance > 126) continue

      const alpha = (1 - distance / 126) * 0.14
      ctx.strokeStyle = `rgba(0, 0, 0, ${alpha})`
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(a.x, a.y)
      ctx.lineTo(b.x, b.y)
      ctx.stroke()
    }
  }
}

const updateParticles = () => {
  const repulseRadius = 120

  particles.forEach((particle) => {
    if (pointer.active) {
      const dx = particle.x - pointer.x
      const dy = particle.y - pointer.y
      const distance = Math.hypot(dx, dy)
      if (distance > 0 && distance < repulseRadius) {
        const force = (1 - distance / repulseRadius) * 0.028
        particle.vx += (dx / distance) * force
        particle.vy += (dy / distance) * force
      }
    }

    particle.x += particle.vx
    particle.y += particle.vy
    particle.vx *= 0.992
    particle.vy *= 0.992

    if (Math.abs(particle.vx) < 0.06) {
      particle.vx += (Math.random() - 0.5) * 0.04
    }

    if (Math.abs(particle.vy) < 0.06) {
      particle.vy += (Math.random() - 0.5) * 0.04
    }

    if (particle.x < -20) particle.x = width + 20
    if (particle.x > width + 20) particle.x = -20
    if (particle.y < -20) particle.y = height + 20
    if (particle.y > height + 20) particle.y = -20
  })
}

const drawParticles = () => {
  if (!ctx) return

  ctx.clearRect(0, 0, width, height)
  drawConnections()

  particles.forEach((particle) => {
    ctx.fillStyle = 'rgba(0, 0, 0, 0.64)'
    ctx.beginPath()
    ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2)
    ctx.fill()
  })
}

const tick = () => {
  updateParticles()
  drawParticles()
  rafId = window.requestAnimationFrame(tick)
}

const handleMove = (event) => {
  pointer.x = event.clientX
  pointer.y = event.clientY
  pointer.active = true
}

const handleLeave = () => {
  pointer.active = false
}

onMounted(() => {
  resetCanvas()
  window.addEventListener('resize', resetCanvas)
  window.addEventListener('mousemove', handleMove)
  window.addEventListener('mouseleave', handleLeave)
  rafId = window.requestAnimationFrame(tick)
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(rafId)
  window.removeEventListener('resize', resetCanvas)
  window.removeEventListener('mousemove', handleMove)
  window.removeEventListener('mouseleave', handleLeave)
  resizeObserver?.disconnect?.()
})
</script>

<style scoped>
.background-particles {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  opacity: 0.9;
}
</style>
