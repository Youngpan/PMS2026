<template>
  <el-card>
    <template #header>
      <div style="display:flex; justify-content:space-between; align-items:center">
        <span>账单 #{{ folio.folio?.id }} · 余额 ¥{{ folio.folio?.balance }} · {{ folio.folio?.status }}</span>
        <el-button @click="load">刷新</el-button>
      </div>
    </template>
    <el-table :data="folio.items" border stripe>
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="category" label="项目" width="120" />
      <el-table-column prop="amount" label="金额" width="100" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column prop="createdAt" label="时间" width="180" />
    </el-table>

    <div style="margin-top:16px; display:flex; gap:8px">
      <el-button @click="dialog.charge = true">录消费</el-button>
      <el-button @click="dialog.deposit = true">录押金</el-button>
      <el-button type="primary" @click="dialog.pay = true">收款</el-button>
      <el-button @click="receipt">打印收据</el-button>
    </div>

    <el-dialog v-model="dialog.charge" title="录消费" width="360px">
      <el-form label-width="80px">
        <el-form-item label="项目"><el-input v-model="form.charge.category" /></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="form.charge.amount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.charge.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog.charge=false">取消</el-button><el-button type="primary" @click="addCharge">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="dialog.deposit" title="录押金" width="360px">
      <el-form label-width="80px">
        <el-form-item label="金额"><el-input-number v-model="form.deposit.amount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="方式"><el-select v-model="form.deposit.method"><el-option label="现金" value="CASH" /><el-option label="微信" value="WECHAT" /><el-option label="支付宝" value="ALIPAY" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog.deposit=false">取消</el-button><el-button type="primary" @click="addDeposit">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="dialog.pay" title="收款" width="360px">
      <el-form label-width="80px">
        <el-form-item label="应收">¥{{ folio.folio?.balance }}</el-form-item>
        <el-form-item label="金额"><el-input-number v-model="form.pay.amount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="方式"><el-select v-model="form.pay.method"><el-option label="现金" value="CASH" /><el-option label="微信" value="WECHAT" /><el-option label="支付宝" value="ALIPAY" /><el-option label="银行卡" value="CARD" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog.pay=false">取消</el-button><el-button type="primary" @click="pay">收款</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { folioApi } from '@/api/modules'

const route = useRoute()
const id = Number(route.params.id)
const folio = ref<any>({ folio: {}, items: [] })
const dialog = reactive({ charge: false, deposit: false, pay: false })
const form = reactive<any>({
  charge: { category: '杂费', amount: 0, remark: '' },
  deposit: { amount: 0, method: 'CASH' },
  pay: { amount: 0, method: 'WECHAT' },
})

async function load() { folio.value = await folioApi.detail(id) }
async function addCharge() { await folioApi.addCharge(id, form.charge); dialog.charge = false; load() }
async function addDeposit() { await folioApi.addDeposit(id, form.deposit); dialog.deposit = false; load() }
async function pay() { await folioApi.pay(id, form.pay.method, form.pay.amount); dialog.pay = false; load() }
async function receipt() {
  const r: any = await folioApi.receipt(id)
  ElMessageBox.alert(r.text, '收据', { confirmButtonText: '关闭' })
}
onMounted(load)
</script>
