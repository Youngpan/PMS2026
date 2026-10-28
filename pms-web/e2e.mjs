// 前端 e2e 联调：用真实 Chrome 走 登录→仪表盘→各业务页，截图 + 抓 console 错误。
import puppeteer from 'puppeteer-core'
import { mkdirSync, existsSync } from 'node:fs'

const CHROME = process.env.PMS_CHROME || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const BASE = 'http://localhost:5173'
const SHOTS = 'E:\\PMS\\tools\\shots'
if (!existsSync(SHOTS)) mkdirSync(SHOTS, { recursive: true })

const errors = []
const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: 'new',
  args: ['--no-sandbox', '--disable-gpu', '--window-size=1440,900'],
})
const page = await browser.newPage()
await page.setViewport({ width: 1440, height: 900 })
page.on('console', (m) => { if (m.type() === 'error') errors.push(`[console] ${m.text()}`) })
page.on('pageerror', (e) => errors.push(`[pageerror] ${e.message}`))
page.on('requestfailed', (r) => errors.push(`[reqfail] ${r.url()} ${r.failure()?.errorText}`))

async function shot(name) {
  await new Promise((r) => setTimeout(r, 1200))
  await page.screenshot({ path: `${SHOTS}\\${name}.png`, fullPage: false })
  console.log('shot:', name)
}

console.log('1. 打开登录页')
await page.goto(`${BASE}/login`, { waitUntil: 'networkidle0', timeout: 30000 })
await shot('01-login')

console.log('2. 登录（表单默认 admin/admin）')
// Login.vue 表单默认即 admin/admin，无需键入（clickCount:3+type 在已有值上会追加导致密码变 adminadmin）
// SPA pushState 不触发 puppeteer waitForNavigation，改用 waitForFunction 等 URL 离开 /login
await page.click('button.el-button--primary')
await page.waitForFunction(() => !location.pathname.includes('/login'), { timeout: 15000 })
console.log('   登录后 URL:', page.url())
await shot('02-dashboard')

const pages = [
  ['reservations', '/reservations'],
  ['stays', '/stays'],
  ['housekeeping', '/housekeeping'],
  ['reports', '/reports'],
  ['staff', '/system/staff'],
  ['dashboard', '/dashboard'],
]
console.log('3. 遍历业务页 + 抓文本')
for (const [name, url] of pages) {
  await page.goto(`${BASE}${url}`, { waitUntil: 'networkidle0', timeout: 20000 }).catch((e) => errors.push(`[goto:${name}] ${e.message}`))
  await new Promise((r) => setTimeout(r, 900))
  await shot(`0${pages.indexOf([name, url]) + 3}-${name}`)
  const txt = await page.evaluate(() => document.querySelector('.el-main')?.innerText?.slice(0, 240) || '(empty el-main)')
  console.log(`   ${name}: ${txt.replace(/\s+/g, ' ').slice(0, 200)}`)
}

await browser.close()
console.log('\n=== 错误汇总 ===')
if (errors.length === 0) console.log('无 console / 页面错误')
else errors.forEach((e) => console.log(e))
console.log('\nDONE')
