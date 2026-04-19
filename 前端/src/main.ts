import { createApp } from 'vue'
import { createPinia } from 'pinia'
import naive from 'naive-ui'
import App from './App.vue'
import router from './router'
import { progressiveBlurScrollDirective } from './directives/progressiveBlurScroll'

import './styles.css'
import './styles/animations.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(naive)
app.directive('progressive-blur-scroll', progressiveBlurScrollDirective)

app.mount('#app')
