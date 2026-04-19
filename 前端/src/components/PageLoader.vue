<template>
  <Transition name="page-loader-fade">
    <div v-if="active" class="page-loader" aria-live="polite" aria-busy="true">
      <div class="page-loader__backdrop"></div>
      <div class="page-loader__aurora page-loader__aurora--one"></div>
      <div class="page-loader__aurora page-loader__aurora--two"></div>
      <div class="page-loader__constellation page-loader__constellation--left"></div>
      <div class="page-loader__constellation page-loader__constellation--right"></div>

      <div class="page-loader__panel">
        <div class="page-loader__orbit">
          <span class="page-loader__ring page-loader__ring--outer"></span>
          <span class="page-loader__ring page-loader__ring--middle"></span>
          <span class="page-loader__ring page-loader__ring--inner"></span>
          <span class="page-loader__core">文</span>
          <span class="page-loader__satellite page-loader__satellite--one"></span>
          <span class="page-loader__satellite page-loader__satellite--two"></span>
          <span class="page-loader__satellite page-loader__satellite--three"></span>
        </div>

        <div class="page-loader__text">
          <strong>{{ title }}</strong>
          <p>{{ message }}</p>
        </div>

        <div class="page-loader__progress">
          <span></span>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
defineProps({
  active: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '正在载入书苑',
  },
  message: {
    type: String,
    default: '页面内容与动效资源正在整理中，请稍候。',
  },
})
</script>

<style scoped>
.page-loader {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: grid;
  place-items: center;
  overflow: hidden;
  pointer-events: all;
}

.page-loader__backdrop,
.page-loader__aurora,
.page-loader__constellation {
  position: absolute;
  inset: 0;
}

.page-loader__backdrop {
  background:
    radial-gradient(circle at 22% 24%, rgba(251, 205, 96, 0.16), transparent 22%),
    radial-gradient(circle at 78% 16%, rgba(79, 142, 214, 0.24), transparent 24%),
    linear-gradient(135deg, rgba(5, 17, 58, 0.96), rgba(12, 32, 74, 0.94) 48%, rgba(20, 59, 101, 0.92));
  backdrop-filter: blur(12px);
}

.page-loader__aurora {
  filter: blur(10px);
  opacity: 0.7;
}

.page-loader__aurora--one {
  background: radial-gradient(circle at 25% 65%, rgba(255, 179, 71, 0.26), transparent 26%);
  animation: loaderAuroraDrift 9s ease-in-out infinite;
}

.page-loader__aurora--two {
  background: radial-gradient(circle at 75% 30%, rgba(61, 130, 255, 0.22), transparent 28%);
  animation: loaderAuroraDrift 11s ease-in-out infinite reverse;
}

.page-loader__constellation {
  background-image:
    radial-gradient(circle at 10% 20%, rgba(255, 255, 255, 0.6) 0 1px, transparent 1.5px),
    radial-gradient(circle at 22% 74%, rgba(255, 255, 255, 0.48) 0 1px, transparent 1.5px),
    radial-gradient(circle at 80% 18%, rgba(255, 255, 255, 0.52) 0 1px, transparent 1.5px),
    radial-gradient(circle at 72% 68%, rgba(255, 255, 255, 0.5) 0 1px, transparent 1.5px);
  opacity: 0.5;
}

.page-loader__constellation::before,
.page-loader__constellation::after {
  content: '';
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 999px;
}

.page-loader__constellation--left::before {
  width: 240px;
  height: 240px;
  left: -90px;
  top: 16%;
}

.page-loader__constellation--left::after {
  width: 120px;
  height: 120px;
  left: 64px;
  top: 26%;
}

.page-loader__constellation--right::before {
  width: 220px;
  height: 220px;
  right: -84px;
  bottom: 14%;
}

.page-loader__constellation--right::after {
  width: 110px;
  height: 110px;
  right: 84px;
  bottom: 26%;
}

.page-loader__panel {
  position: relative;
  z-index: 1;
  width: min(420px, calc(100vw - 40px));
  padding: 32px 28px 28px;
  border-radius: 30px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.02));
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(18px);
  display: grid;
  gap: 22px;
  justify-items: center;
}

.page-loader__orbit {
  position: relative;
  width: 176px;
  height: 176px;
  display: grid;
  place-items: center;
}

.page-loader__ring,
.page-loader__satellite {
  position: absolute;
  border-radius: 50%;
}

.page-loader__ring {
  inset: 0;
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.page-loader__ring--outer {
  animation: loaderSpin 8s linear infinite;
}

.page-loader__ring--middle {
  inset: 16px;
  border-style: dashed;
  border-color: rgba(255, 208, 112, 0.32);
  animation: loaderSpinReverse 6s linear infinite;
}

.page-loader__ring--inner {
  inset: 34px;
  border-color: rgba(131, 201, 255, 0.34);
  animation: loaderPulseRing 2.4s ease-in-out infinite;
}

.page-loader__core {
  width: 74px;
  height: 74px;
  border-radius: 24px;
  display: grid;
  place-items: center;
  font-family: 'STSong', 'SimSun', serif;
  font-size: 30px;
  font-weight: 800;
  color: #fff;
  background:
    radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.38), transparent 42%),
    linear-gradient(135deg, rgba(255, 192, 78, 0.95), rgba(183, 96, 55, 0.94));
  box-shadow:
    0 18px 40px rgba(183, 96, 55, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.35);
  animation: loaderCoreFloat 2.6s ease-in-out infinite;
}

.page-loader__satellite {
  width: 12px;
  height: 12px;
  background: #fff;
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.4);
}

.page-loader__satellite--one {
  top: 14px;
  left: 50%;
  margin-left: -6px;
  background: #ffd37c;
  animation: loaderSatelliteOne 3.2s linear infinite;
}

.page-loader__satellite--two {
  right: 18px;
  bottom: 32px;
  background: #8cd0ff;
  animation: loaderSatelliteTwo 2.8s ease-in-out infinite;
}

.page-loader__satellite--three {
  left: 22px;
  bottom: 22px;
  background: rgba(255, 255, 255, 0.86);
  animation: loaderSatelliteThree 2.5s ease-in-out infinite;
}

.page-loader__text {
  text-align: center;
  color: #fff;
}

.page-loader__text strong {
  display: block;
  font-size: 22px;
  letter-spacing: 0.04em;
}

.page-loader__text p {
  margin: 8px 0 0;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
}

.page-loader__progress {
  width: 100%;
  height: 8px;
  padding: 1px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.page-loader__progress span {
  display: block;
  width: 40%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(255, 208, 112, 0.3), rgba(255, 208, 112, 0.95), rgba(140, 208, 255, 0.9));
  box-shadow: 0 0 24px rgba(255, 208, 112, 0.4);
  animation: loaderProgress 1.5s cubic-bezier(0.55, 0.1, 0.45, 0.9) infinite;
}

.page-loader-fade-enter-active,
.page-loader-fade-leave-active {
  transition: opacity 0.3s ease;
}

.page-loader-fade-enter-from,
.page-loader-fade-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .page-loader__panel {
    padding: 26px 20px 22px;
    gap: 18px;
  }

  .page-loader__orbit {
    width: 154px;
    height: 154px;
  }
}
</style>
