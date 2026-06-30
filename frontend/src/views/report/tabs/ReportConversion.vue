<template>
  <div class="tab-content">
    <!-- 5 KPI 密集条(跟进总数 + 跟进率 + 客户转换率 + 商机转换率 + 合同转换率) -->
    <KpiStrip :kpis="data.kpis || []" />

    <!-- 2x2 图表:转化漏斗 / 高频跟进人 -->
    <ChartGrid2x2>
      <ChartCard title="阶段转化漏斗" :meta="`5 阶段 · ${data.stageFunnel?.length || 0}`">
        <ChartFunnel
          :data="(data.stageFunnel || []).map(s => ({ name: s.stageName, value: s.count }))"
          :height="280"
        />
      </ChartCard>
      <ReportDataTable
        title="高频跟进人 · TOP N"
        meta="按跟进条数"
        :columns="[
          { key: 'rank',  title: '排名' },
          { key: 'name',  title: '跟进人' },
          { key: 'count', title: '条数', align: 'right' }
        ]"
        :rows="data.topPerformers || []"
      />
    </ChartGrid2x2>
  </div>
</template>

<script setup>
import KpiStrip from '@/components/report/KpiStrip.vue'
import ChartCard from '@/components/report/ChartCard.vue'
import ChartGrid2x2 from '@/components/report/ChartGrid2x2.vue'
import ReportDataTable from '@/components/report/ReportDataTable.vue'
import ChartFunnel from '@/components/report/charts/ChartFunnel.vue'

const props = defineProps({
  data: { type: Object, required: true }
})
</script>

<style lang="scss" scoped>
.tab-content { width: 100%; }
</style>