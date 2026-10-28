// 前端全链路 e2e：登录 → 新建预订 → 确认 → 入住 → 退房，截图 + 抓 console 错误。
// 用真实 Chrome 驱动 Element Plus 表单/弹窗/表格，验证前后端联调无回归。
// 前置：MySQL(3306)+后端(8080)+前端(5173) 在跑，且数据已 reset 到 VC 净房。
import puppeteer from 'puppeteer-core'
import { mkdirSync, existsSync } from 'node:fs'

const CHROME = process.env.PMS_CHROME || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const BASE = 'http://localhost:5173'
const SHOTS = 'E:\\PMS\\tools\\shots\\flow'
if (!existsSync(SHOTS)) mkdirSync(SHOTS, { recursive: true })

const errors = []
const steps = []
function log(msg) { console.log(msg); steps.push(msg) }

const today = new Date()
const tomorrow = new Date(Date.now() + 86400000)
const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const GUEST = 'E2E' + Date.now().toString().slice(-6)

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
  await new Promise((r) => setTimeout(r, 900))
  await page.screenshot({ path: `${SHOTS}\\${name}.png`, fullPage: false })
}
async function wait(ms) { return new Promise((r) => setTimeout(r, ms)) }

// 在 el-table 行中按文本找到含指定按钮文本的按钮并点击
async function clickInRow(rowText, btnText) {
  const clicked = await page.evaluate((rt, bt) => {
    const rows = document.querySelectorAll('.el-table__row')
    for (const r of rows) {
      if (r.innerText.includes(rt)) {
        const btn = Array.from(r.querySelectorAll('button')).find(b => b.innerText.trim() === bt)
        if (btn) { btn.click(); return true }
      }
    }
    return false
  }, rowText, btnText)
  if (!clicked) throw new Error(`未找到含 "${rowText}" 的行的 "${btnText}" 按钮`)
}

async function waitForRow(rowText, timeout = 12000) {
  await page.waitForFunction(
    (t) => Array.from(document.querySelectorAll('.el-table__row')).some(r => r.innerText.includes(t)),
    { timeout }, rowText
  )
}

async function getRowStatus(rowText) {
  return page.evaluate((t) => {
    const rows = document.querySelectorAll('.el-table__row')
    for (const r of rows) {
      if (r.innerText.includes(t)) {
        const tag = r.querySelector('.el-tag')
        return tag ? tag.innerText.trim() : '(无状态标签)'
      }
    }
    return '(未找到行)'
  }, rowText)
}

async function waitForMessageBox(timeout = 8000) {
  await page.waitForSelector('.el-message-box', { visible: true, timeout })
  await wait(300)
}

async function pickDate(idx, dayNum) {
  // el-date-picker：点前缀日历图标打开面板 → 在【可见】面板里按 day 文本点单元格。
  // EP 选定日期后自动关闭面板；用 offsetParent 锁定当前可见面板，避免命中残留面板。
  // 注意：不能用 Escape 关残留面板——EP el-dialog 默认 close-on-press-escape 会把整个对话框关掉。
  await page.evaluate((i) => {
    document.querySelectorAll('.el-dialog .el-date-editor')[i]?.querySelector('.el-input__prefix')?.click()
  }, idx)
  await wait(500)
  const clicked = await page.evaluate((d) => {
    const panels = document.querySelectorAll('.el-picker-panel')
    let panel = null
    for (const p of panels) if (p.offsetParent !== null) { panel = p; break }
    if (!panel) return false
    for (const td of panel.querySelectorAll('.el-date-table td')) {
      if (td.classList.contains('prev-month') || td.classList.contains('next-month')) continue
      if (td.classList.contains('disabled')) continue
      if (td.textContent?.trim() === String(d)) { td.click(); return true }
    }
    return false
  }, dayNum)
  if (!clicked) throw new Error(`未找到日期单元格 day=${dayNum}`)
  await wait(300)
}

async function selectFirstRoomType() {
  // 用 evaluate 点 el-select 触发器（wrapper），避免 page.click 指针拦截
  await page.evaluate(() => {
    const sel = document.querySelector('.el-dialog .el-select')
    const trigger = sel?.querySelector('.el-select__wrapper') || sel?.querySelector('.el-input') || sel
    trigger?.click()
  })
  await page.waitForSelector('.el-select-dropdown__item', { visible: true, timeout: 5000 })
  await page.evaluate(() => {
    const opts = Array.from(document.querySelectorAll('.el-select-dropdown__item'))
    const vis = opts.find(o => o.offsetParent !== null) || opts[0]
    if (vis) vis.click()
  })
  await wait(300)
}

// ---------- 流程 ----------
try {
  log('1. 登录')
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle0', timeout: 30000 })
  // Login.vue 表单默认即 admin/admin，无需键入（clickCount:3+type 在已有值上会追加导致密码变 adminadmin）
  // SPA pushState 不触发 puppeteer waitForNavigation，改用 waitForFunction 等 URL 离开 /login
  await page.click('button.el-button--primary')
  await page.waitForFunction(() => !location.pathname.includes('/login'), { timeout: 15000 })
  await shot('01-dashboard')

  log('2. 打开预订页 → 新建预订对话框')
  await page.goto(`${BASE}/reservations`, { waitUntil: 'networkidle0', timeout: 20000 })
  await wait(500)
  await page.evaluate(() => {
    const btn = Array.from(document.querySelectorAll('button')).find(b => b.innerText.trim() === '新建预订')
    if (btn) btn.click()
  })
  await page.waitForSelector('.el-dialog', { visible: true, timeout: 5000 })
  await wait(400)

  log(`3. 填写表单（宾客=${GUEST}）`)
  const dialogInputs = await page.$$('.el-dialog .el-input__inner')
  log(`   对话框 input 数: ${dialogInputs.length}`)
  await dialogInputs[0].click({ clickCount: 3 }); await page.keyboard.type(GUEST)
  await dialogInputs[1].click({ clickCount: 3 }); await page.keyboard.type('1390000' + GUEST.slice(-5))
  log('   姓名电话已填')
  await pickDate(0, today.getDate())
  await pickDate(1, tomorrow.getDate())
  log('   日期已填')
  await selectFirstRoomType()
  log('   房型已选')
  await wait(300)
  await shot('02-create-form')

  log('4. 保存预订')
  await page.evaluate(() => {
    const btn = Array.from(document.querySelectorAll('.el-dialog button')).find(b => b.innerText.trim() === '保存')
    if (btn) btn.click()
  })
  await page.waitForFunction(() => !document.querySelector('.el-dialog') || getComputedStyle(document.querySelector('.el-dialog')).display === 'none', { timeout: 8000 }).catch(() => null)
  await waitForRow(GUEST)
  await shot('03-reservation-created')

  let status = await getRowStatus(GUEST)
  log(`   新建后状态: ${status}`)
  if (!status.includes('待确认')) throw new Error(`新建预订状态异常: ${status}`)

  log('5. 确认预订')
  await clickInRow(GUEST, '确认')
  await wait(1000)
  await shot('04-reservation-confirmed')
  status = await getRowStatus(GUEST)
  log(`   确认后状态: ${status}`)
  if (!status.includes('已确认')) throw new Error(`确认后状态异常: ${status}`)

  log('6. 办理入住')
  await clickInRow(GUEST, '入住')
  await waitForMessageBox()
  await page.evaluate(() => {
    const btn = Array.from(document.querySelectorAll('.el-message-box__btns button')).find(b => b.innerText.trim() === '确定')
    if (btn) btn.click()
  })
  await wait(1500)
  await shot('05-checked-in')
  status = await getRowStatus(GUEST)
  log(`   入住后状态: ${status}`)
  if (!status.includes('已入住')) throw new Error(`入住后状态异常: ${status}`)

  log('7. 在住看板 → 退房')
  await page.goto(`${BASE}/stays`, { waitUntil: 'networkidle0', timeout: 20000 })
  await wait(500)
  // 找到包含该宾客的 stay 行困难（在住看板只显示 stayId/roomId/guestId），改用第一行的退房按钮
  const stayRow = await page.$('.el-table__row')
  if (!stayRow) throw new Error('在住看板无在住记录')
  await page.evaluate(() => {
    const row = document.querySelector('.el-table__row')
    const btn = Array.from(row.querySelectorAll('button')).find(b => b.innerText.trim() === '退房')
    if (btn) btn.click()
  })
  await waitForMessageBox()
  // ElMessageBox.prompt：输入金额 1（>0 触发 pay 关账；押金 500 → 余额 -499 ≤0 → CLOSED）
  await page.waitForSelector('.el-message-box__input .el-input__inner', { visible: true, timeout: 5000 })
  await page.click('.el-message-box__input .el-input__inner', { clickCount: 3 })
  await page.keyboard.type('1')
  await page.evaluate(() => {
    const btn = Array.from(document.querySelectorAll('.el-message-box__btns button')).find(b => b.innerText.trim() === '确定')
    if (btn) btn.click()
  })
  await wait(1500)
  await shot('06-checked-out')

  log('8. 回到预订页验证最终状态')
  await page.goto(`${BASE}/reservations`, { waitUntil: 'networkidle0', timeout: 20000 })
  await wait(500)
  await waitForRow(GUEST)
  await shot('07-reservation-final')
  status = await getRowStatus(GUEST)
  log(`   最终状态: ${status}`)
  if (!status.includes('已退房')) throw new Error(`退房后状态异常: ${status}`)

  log('\n✅ 全链路通过：新建→确认→入住→退房')
} catch (e) {
  errors.push(`[flow] ${e.message}`)
  await shot('99-error')
} finally {
  await browser.close()
}

console.log('\n=== 错误汇总 ===')
if (errors.length === 0) console.log('无 console / 页面 / 流程错误')
else errors.forEach((e) => console.log(e))
console.log('\nDONE guest=' + GUEST)
process.exit(errors.length ? 1 : 0)
