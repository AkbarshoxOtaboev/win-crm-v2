<template>
  <AdminLayout>
    <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div class="flex min-w-0 flex-wrap items-center gap-2.5">
        <h2 class="text-xl font-semibold text-gray-800 dark:text-white/90">
          {{ order ? t('saleOrderDetail.title', { no: order.orderNumber || order.id }) : t('saleOrderDetail.titleFallback') }}
        </h2>
        <router-link
          v-if="order?.clientFullName && order.clientId"
          :to="{ path: `/clients/${order.clientId}`, query: { tab: 'act' } }"
          class="meta-chip meta-chip-link"
          :title="t('clientDetail.tabs.act')"
        >
          <UserRound class="h-3.5 w-3.5 shrink-0" />
          {{ order.clientFullName }}
        </router-link>
        <span v-else-if="order?.clientFullName" class="meta-chip">
          <UserRound class="h-3.5 w-3.5 shrink-0" />
          {{ order.clientFullName }}
        </span>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <button
          v-if="order"
          type="button"
          class="ghost btn-with-icon"
          :disabled="refreshing"
          :title="t('common.refresh')"
          @click="onRefresh"
        >
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': refreshing }" />
          {{ t('common.refresh') }}
        </button>
        <nav>
          <ol class="flex flex-wrap items-center gap-1.5">
            <li class="flex items-center gap-1.5">
              <router-link class="text-sm text-gray-500 hover:text-brand-500 dark:text-gray-400" to="/">{{ t('nav.home') }}</router-link>
              <span class="text-sm text-gray-400">/</span>
            </li>
            <li class="flex items-center gap-1.5">
              <router-link class="text-sm text-gray-500 hover:text-brand-500 dark:text-gray-400" to="/sales">{{ t('nav.salesOrders') }}</router-link>
              <span class="text-sm text-gray-400">/</span>
            </li>
            <li class="text-sm text-gray-800 dark:text-white/90">
              {{ order ? t('saleOrderDetail.title', { no: order.orderNumber || order.id }) : t('saleOrderDetail.titleFallback') }}
            </li>
          </ol>
        </nav>
      </div>
    </div>
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div v-if="order" class="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6">
      <article v-for="card in summaryCards" :key="card.key" class="info-card">
        <span class="info-icon" :class="card.tone">
          <component :is="card.icon" class="h-4 w-4" />
        </span>
        <div class="min-w-0">
          <p class="info-lbl">{{ card.label }}</p>
          <p class="info-val">{{ card.value }}</p>
          <p v-if="card.hint" class="info-hint">{{ card.hint }}</p>
        </div>
      </article>
    </div>
    <p v-if="order?.comment" class="mb-4 text-sm text-gray-500 dark:text-gray-400">
      <span class="font-medium text-gray-600 dark:text-gray-300">{{ t('common.comment') }}:</span>
      <span class="whitespace-pre-line"> {{ order.comment }}</span>
    </p>

    <div v-if="order" class="card mb-4 p-5">
      <div class="status-head">
        <div>
          <h3 class="title">{{ t('saleOrderDetail.changeStatus') }}</h3>
          <p class="sub">{{ t('saleOrderDetail.currentStatus') }} <SaleStatusBadge :status="order.orderStatus" /></p>
          <p class="status-click-hint">{{ t('saleOrderDetail.statusClickHint') }}</p>
        </div>
        <div v-if="deadlineBanner" class="deadline-banner" :class="deadlineBanner.tone">
          <span class="deadline-banner-lbl">{{ t('saleOrderDetail.dueDate') }}</span>
          <span class="deadline-banner-val">{{ deadlineBanner.text }}</span>
        </div>
      </div>
      <div class="status-flow">
        <template v-for="(st, i) in flowStatuses" :key="st">
          <span v-if="i > 0" class="flow-sep" :class="{ done: doneIndex >= i || (st === 'CANCELLED' && order.orderStatus === 'CANCELLED') }" />
          <button
            type="button"
            class="flow-step"
            :class="{
              done: doneIndex >= i,
              current: flowIndex === i,
              cancelled: st === 'CANCELLED',
              clickable: canClickStatus(st),
            }"
            :disabled="!canClickStatus(st)"
            :title="canClickStatus(st) ? statusButtonLabel(st) : undefined"
            @click="onStatus(st)"
          >
            <span class="flow-step-label">{{ statusLabel(st) }}</span>
            <span class="flow-step-date">{{ statusReachedAt(st) }}</span>
            <span v-if="st === 'COMPLETED' && deadlineCompareText" class="flow-step-meta" :class="deadlineCompareTone">
              {{ deadlineCompareText }}
            </span>
          </button>
        </template>
      </div>
      <p v-if="isFinalOrder" class="mt-4 text-sm text-gray-500 dark:text-gray-400">{{ t('saleOrderDetail.noNextStatus') }}</p>
      <p v-if="order.orderStatus === 'PROCESSING'" class="mt-2 text-xs text-gray-500 dark:text-gray-400">
        {{ t('saleOrderDetail.processingHint', { status: statusLabel('READY') }) }}
      </p>
      <p v-if="order.orderStatus === 'READY'" class="mt-2 text-xs text-gray-500 dark:text-gray-400">
        {{ t('saleOrderDetail.readyHint', { inDelivery: statusLabel('IN_DELIVERY'), delivered: statusLabel('DELIVERED') }) }}
      </p>
      <p v-if="order.orderStatus === 'IN_DELIVERY' && deliveryConfirmBlocked" class="mt-2 text-xs text-amber-600 dark:text-amber-400">
        {{ t('saleOrderDetail.deliveryConfirmBlocked') }}
      </p>
      <p v-if="order.orderStatus === 'WORK_DONE'" class="mt-2 text-xs" :class="completeBlocked ? 'text-amber-600 dark:text-amber-400' : 'text-gray-500 dark:text-gray-400'">
        {{
          completeBlocked
            ? t('saleOrderDetail.completeBlockedDebt', { debt: amt(order.debtSum) })
            : t('saleOrderDetail.completeReady', { status: statusLabel('COMPLETED') })
        }}
      </p>
    </div>

    <div v-if="production && production.route && production.route.length" class="card mb-4 p-5">
      <div class="status-head">
        <div>
          <h3 class="title">{{ t('saleOrderDetail.workshopRoute') }}</h3>
          <p class="sub">{{ t('saleOrderDetail.workshopRouteHint') }}</p>
        </div>
      </div>
      <ol class="route-list mt-4">
        <li v-for="s in production.route" :key="s.stepNo" class="route-step" :class="s.state.toLowerCase()">
          <span class="route-no">
            <Check v-if="s.state === 'DONE'" class="h-3.5 w-3.5" />
            <template v-else>{{ s.stepNo }}</template>
          </span>
          <span class="route-name">{{ s.workshopName }}</span>
          <span class="route-state">{{ t(`saleOrderDetail.routeState.${s.state}`) }}</span>
        </li>
      </ol>
    </div>

    <div v-if="delivery" class="card mb-4 p-5">
      <div class="status-head">
        <div>
          <h3 class="title">{{ t('saleOrderDetail.delivery') }}</h3>
          <p class="sub">{{ t('saleOrderDetail.statusPrefix') }} <DeliveryStatusBadge :status="delivery.deliveryStatus" /></p>
        </div>
      </div>
      <div class="mt-4 grid grid-cols-2 gap-3 text-sm md:grid-cols-4">
        <div><span class="lbl">{{ t('saleOrderDetail.driver') }}</span>{{ delivery.driverFullName || '—' }}<span v-if="delivery.driverPhone" class="block text-xs text-gray-500">{{ delivery.driverPhone }}</span></div>
        <div><span class="lbl">{{ t('saleOrderDetail.car') }}</span>{{ [delivery.carModel, delivery.carNumber].filter(Boolean).join(' · ') || '—' }}</div>
        <div class="col-span-2"><span class="lbl">{{ t('saleOrderDetail.workers') }}</span>{{ delivery.workers.map((w) => w.fullName).join(', ') || '—' }}</div>
        <div class="col-span-2"><span class="lbl">{{ t('common.address') }}</span>{{ delivery.address || '—' }}</div>
        <div><span class="lbl">{{ t('saleOrderDetail.sentAt') }}</span>{{ formatDate(delivery.sentAt) }}</div>
        <div><span class="lbl">{{ t('saleOrderDetail.acceptedAt') }}</span>{{ formatDate(delivery.acceptedAt) }}<span v-if="delivery.acceptedByName" class="block text-xs text-gray-500">{{ delivery.acceptedByName }}</span></div>
        <div><span class="lbl">{{ t('saleOrderDetail.departedAt') }}</span>{{ formatDate(delivery.departedAt) }}</div>
        <div><span class="lbl">{{ t('saleOrderDetail.arrivedAt') }}</span>{{ formatDate(delivery.arrivedAt) }}</div>
        <div><span class="lbl">{{ t('saleOrderDetail.confirmedAt') }}</span>{{ formatDate(delivery.confirmedAt) }}<span v-if="delivery.confirmedByName" class="block text-xs text-gray-500">{{ delivery.confirmedByName }}</span></div>
        <div v-if="delivery.salaryTotal != null"><span class="lbl">{{ t('saleOrderDetail.workersSalary') }}</span>{{ money(delivery.salaryTotal) }} ({{ delivery.salaryPercent }}%)</div>
        <div v-if="delivery.note" class="col-span-2"><span class="lbl">{{ t('common.note') }}</span>{{ delivery.note }}</div>
      </div>
    </div>

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <template v-for="tb in tabs" :key="tb.id">
        <button type="button" class="tab" :class="{ active: tab === tb.id }" @click="tab = tb.id">{{ tb.label }}</button>
        <router-link
          v-if="tb.id === 'items' && order"
          :to="`/sales/${order.id}/optimize`"
          class="tab"
        >
          {{ t('cuttingOptimize.open') }}
        </router-link>
      </template>
      <button
        v-if="order && auth.can('SALE_ORDER_EDIT')"
        type="button"
        class="btn btn-with-icon ms-auto"
        :disabled="writeBlocked || order.orderStatus === 'CANCELLED'"
        :title="order.orderStatus === 'CANCELLED' ? t('saleOrderDetail.editBlockedCancelled') : undefined"
        @click="openEdit"
      >
        <Pencil class="h-4 w-4" />
        {{ t('saleOrderDetail.updateOrder') }}
      </button>
    </div>

    <div v-show="tab === 'items'" class="card">
      <div class="head">
        <div class="flex min-w-0 flex-wrap items-center gap-2.5">
          <h3 class="title">{{ t('saleOrderDetail.tabs.items') }}</h3>
          <span v-if="order?.warehouseName" class="meta-chip">
            <Warehouse class="h-3.5 w-3.5 shrink-0" />
            {{ order.warehouseName }}
          </span>
        </div>
      </div>
      <div v-if="order" class="items-meta">
        <span>
          <Scale class="h-3.5 w-3.5" />
          {{ t('saleOrderDetail.weight') }}: <b>{{ formatKg(orderWeight) }}</b>
        </span>
        <span>
          <Truck v-if="order.deliveryType === 'DELIVERY'" class="h-3.5 w-3.5" />
          <PackageCheck v-else class="h-3.5 w-3.5" />
          {{ t('saleOrderCreate.deliveryTitle') }}:
          <b>{{
            order.deliveryType === 'DELIVERY'
              ? t('saleOrderCreate.delivery')
              : order.deliveryType === 'PICKUP'
                ? t('saleOrderCreate.pickup')
                : '—'
          }}</b>
        </span>
        <span v-if="order.deliveryType === 'DELIVERY'">
          {{ t('saleOrderCreate.deliveryFee') }}: <b>{{ amt(order.deliveryFee || 0) }}</b>
        </span>
      </div>
      <div class="overflow-x-auto">
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">{{ t('common.product') }}</th>
            <th class="th">{{ t('saleOrderCreate.itemType') }}</th>
            <th class="th">{{ t('saleOrderCreate.width') }}</th>
            <th class="th">{{ t('saleOrderCreate.height') }}</th>
            <th class="th">{{ t('common.count') }}</th>
            <th class="th">{{ t('saleOrderCreate.kvm') }}</th>
            <th class="th">{{ t('saleOrderDetail.selling') }}</th>
            <th class="th">{{ t('saleOrderDetail.lineSum') }}</th>
            <th class="th">{{ t('saleOrderCreate.weight') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="items.length === 0"><td colspan="10" class="empty">{{ t('saleOrderDetail.noItems') }}</td></tr>
          <tr v-for="(it, idx) in items" :key="it.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ idx + 1 }}</td>
            <td class="td">{{ itemProductLabel(it) }}</td>
            <td class="td">
              <span class="kind-badge" :class="isServiceGoodsId(it.goodsId) ? 'service' : 'product'">
                {{ isServiceGoodsId(it.goodsId) ? t('saleOrderCreate.kindService') : t('saleOrderCreate.kindProduct') }}
              </span>
            </td>
            <td class="td">{{ isWindowGoodsId(it.goodsId) ? formatQty(it.width) : '—' }}</td>
            <td class="td">{{ isWindowGoodsId(it.goodsId) ? formatQty(it.height) : '—' }}</td>
            <td class="td">{{ formatInt(itemPieces(it)) }}</td>
            <td class="td">{{ isWindowGoodsId(it.goodsId) ? formatQty(it.count) : '—' }}</td>
            <td class="td">{{ amt(it.priceSelling) }}</td>
            <td class="td font-medium text-gray-800 dark:text-white/90">{{ amt(lineSum(it)) }}</td>
            <td class="td whitespace-nowrap">{{ formatKg(it.weightKg) }}</td>
          </tr>
        </tbody>
        <tfoot v-if="items.length || order">
          <tr class="sum-row">
            <td class="td" colspan="5">{{ t('saleOrderCreate.itemsTotal') }}</td>
            <td class="td">{{ formatInt(itemsPieces) }}</td>
            <td class="td">{{ formatQty(itemsKvm) }}</td>
            <td class="td"></td>
            <td class="td">{{ amt(itemsTotal) }}</td>
            <td class="td whitespace-nowrap">{{ formatKg(itemsWeight) }}</td>
          </tr>
          <tr v-if="order?.deliveryType === 'DELIVERY'" class="sum-row soft">
            <td class="td" colspan="8">{{ t('saleOrderCreate.deliveryFee') }}</td>
            <td class="td">{{ amt(order.deliveryFee || 0) }}</td>
            <td class="td"></td>
          </tr>
          <tr v-if="order" class="sum-row">
            <td class="td" colspan="8">{{ t('common.total') }}</td>
            <td class="td">{{ amt(order.totalSum) }}</td>
            <td class="td"></td>
          </tr>
        </tfoot>
      </table>
      </div>
    </div>

    <div v-show="tab === 'discount'" class="card p-5 space-y-4">
      <div v-if="discountLimit != null" class="limit-card">
        <div>
          {{ t('saleOrderDetail.discountLimit', { percent: formatPercent(discountLimit), amount: amt(discountLimitAmount) }) }}
        </div>
        <div v-if="canBypassDiscountLimit" class="mt-1 text-xs opacity-80">{{ t('saleOrderDetail.discountLimitBypass') }}</div>
      </div>
      <div v-if="roleDiscountLimit != null" class="limit-card">
        {{ t('saleOrderDetail.roleDiscountLimit', { percent: formatPercent(roleDiscountLimit), amount: amt(discountBase * roleDiscountLimit) }) }}
      </div>
      <div v-if="order && Number(order.discountAmount || 0) > 0" class="current-discount">
        <div>
          <div class="text-xs text-gray-500 dark:text-gray-400">{{ t('saleOrderDetail.currentDiscount') }}</div>
          <div class="font-semibold text-gray-800 dark:text-white/90">
            {{ order.discountType ? discountTypeLabel(order.discountType) : '' }}
            {{ order.discountType === 'PERCENTAGE' ? `${order.discountValue}%` : '' }}
            — {{ amt(order.discountAmount) }}
          </div>
        </div>
        <button
          type="button"
          class="btn-revert"
          :disabled="saving || writeBlocked || isFinalOrder"
          :title="isFinalOrder ? t('saleOrderDetail.revertDiscountFinal') : undefined"
          @click="onRemoveDiscount"
        >
          {{ t('saleOrderDetail.revertDiscount') }}
        </button>
      </div>
      <form class="flex flex-wrap items-end gap-2" @submit.prevent="onDiscount">
        <div>
          <label class="lbl">{{ t('common.type') }}</label>
          <select v-model="discountType" class="field w-40">
            <option value="PERCENTAGE">{{ discountTypeLabel('PERCENTAGE') }}</option>
            <option value="FIXED_AMOUNT">{{ discountTypeLabel('FIXED_AMOUNT') }}</option>
          </select>
        </div>
        <div>
          <label class="lbl">{{ t('saleOrderDetail.value') }}{{ discountType === 'FIXED_AMOUNT' ? ` (${curLabel})` : ' (%)' }}</label>
          <input v-model.number="discountValue" type="number" min="0.01" step="0.01" class="field w-32" />
        </div>
        <button type="submit" class="btn" :disabled="saving || discountExceeded">{{ t('common.apply') }}</button>
        <button
          v-if="canDiscountDebt"
          type="button"
          class="btn-debt"
          :disabled="saving"
          :title="t('saleOrderDetail.discountDebtHint', { amount: amt(order?.debtSum) })"
          @click="fillDebtDiscount"
        >
          {{ t('saleOrderDetail.discountDebt', { amount: amt(order?.debtSum) }) }}
        </button>
        <div class="w-full">
          <label class="lbl">{{ t('common.comment') }}</label>
          <textarea
            v-model="discountComment"
            rows="2"
            maxlength="500"
            class="field-area"
            :placeholder="t('saleOrderDetail.discountCommentPlaceholder')"
          ></textarea>
        </div>
      </form>
      <div v-if="discountPreview" class="text-sm" :class="discountExceeded ? 'text-error-600 dark:text-error-400' : 'text-gray-600 dark:text-gray-400'">
        {{ t('saleOrderDetail.discountPreview', { percent: formatPercent(discountPreview.fraction), amount: amt(discountPreview.amount) }) }}
        <template v-if="discountExceeded"> — {{ t('saleOrderDetail.discountExceeded') }}</template>
      </div>
      <table class="min-w-full">
        <thead><tr class="border-b border-gray-100 dark:border-gray-800"><th class="th">{{ t('common.type') }}</th><th class="th">{{ t('saleOrderDetail.value') }}</th><th class="th">{{ t('common.sum') }}</th><th class="th">{{ t('common.comment') }}</th><th class="th">{{ t('saleOrderDetail.discountBy') }}</th><th class="th">{{ t('common.date') }}</th></tr></thead>
        <tbody>
          <tr v-if="discounts.length === 0"><td colspan="6" class="empty">{{ t('saleOrderDetail.noHistory') }}</td></tr>
          <tr v-for="d in discounts" :key="d.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">
              <template v-if="d.discountType">{{ discountTypeLabel(d.discountType) }}</template>
              <span v-else class="text-orange-600 dark:text-orange-400">{{ t('saleOrderDetail.discountReverted') }}</span>
            </td>
            <td class="td">{{ d.discountValue ?? '—' }}</td>
            <td class="td">{{ amt(d.discountAmount) }}</td>
            <td class="td whitespace-pre-line">{{ d.comment || '—' }}</td>
            <td class="td">{{ d.createdUsername || '—' }}</td>
            <td class="td">{{ formatDate(d.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'history'" class="card">
      <table class="min-w-full">
        <thead><tr class="border-b border-gray-100 dark:border-gray-800"><th class="th">{{ t('common.from') }}</th><th class="th">{{ t('saleOrderDetail.historyTo') }}</th><th class="th">{{ t('saleOrderDetail.who') }}</th><th class="th">{{ t('common.comment') }}</th><th class="th">{{ t('common.date') }}</th></tr></thead>
        <tbody>
          <tr v-if="history.length === 0"><td colspan="5" class="empty">{{ t('saleOrderDetail.noHistory') }}</td></tr>
          <tr v-for="h in history" :key="h.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td"><SaleStatusBadge v-if="h.fromStatus" :status="h.fromStatus" /><span v-else>—</span></td>
            <td class="td"><SaleStatusBadge :status="h.toStatus" /></td>
            <td class="td">{{ h.changedByUserFullName || h.changedByUsername || '—' }}</td>
            <td class="td whitespace-pre-line">{{ h.comment || '—' }}</td>
            <td class="td">{{ formatDate(h.changedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'images'" class="card p-5 space-y-4">
      <div class="flex flex-wrap items-center gap-3">
        <label class="btn cursor-pointer" :class="{ 'pointer-events-none opacity-60': uploading || writeBlocked }">
          <ImagePlus :size="16" class="mr-2" />
          {{ uploading ? t('common.loading') : t('saleOrderDetail.uploadImages') }}
          <input
            :key="fileInputKey"
            type="file"
            multiple
            accept="image/jpeg,image/png,image/webp,image/gif"
            class="hidden"
            :disabled="uploading || writeBlocked"
            @change="onUploadImages"
          />
        </label>
        <span class="text-xs text-gray-500 dark:text-gray-400">{{ t('saleOrderDetail.imagesHint') }}</span>
      </div>
      <div v-if="images.length" class="image-grid">
        <figure v-for="img in images" :key="img.id" class="image-card">
          <button type="button" class="image-open" :title="img.originalFileName || ''" @click="openPreview(img)">
            <AuthImage :src="imageSrc(img)" :alt="img.originalFileName || ''" class="image-thumb" />
          </button>
          <figcaption class="image-meta">
            <span class="truncate" :title="img.originalFileName || ''">{{ img.originalFileName || img.fileName }}</span>
            <button type="button" class="image-delete" :title="t('common.delete')" @click="onDeleteImage(img)">
              <Trash2 :size="14" />
            </button>
          </figcaption>
        </figure>
      </div>
      <p v-else class="text-sm text-gray-500 dark:text-gray-400">{{ t('saleOrderDetail.noImages') }}</p>
    </div>

    <div v-if="preview" class="overlay" @click.self="preview = null">
      <div class="preview-box">
        <button type="button" class="preview-close" :aria-label="t('common.close')" @click="preview = null"><X :size="18" /></button>
        <AuthImage :src="imageSrc(preview)" :alt="preview.originalFileName || ''" class="preview-img" />
        <p class="preview-name">{{ preview.originalFileName || preview.fileName }}</p>
      </div>
    </div>

    <div v-if="cancelModal" class="overlay">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="cancel-modal-title">
        <div class="mb-4 flex items-start justify-between gap-3">
          <div>
            <h3 id="cancel-modal-title" class="title">{{ t('saleOrderDetail.cancelTitle', { id: order?.id }) }}</h3>
            <p class="modal-hint mt-1">{{ t('saleOrderDetail.cancelHint') }}</p>
          </div>
          <button type="button" class="close-btn" :aria-label="t('common.close')" @click="cancelModal = false">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div v-if="cancelError" class="err mb-3">{{ cancelError }}</div>
        <form class="space-y-4" @submit.prevent="onCancelConfirm">
          <div>
            <label for="cancel-reason" class="f-lbl">{{ t('saleOrderDetail.cancelReason') }} <span class="req">*</span></label>
            <textarea
              id="cancel-reason"
              v-model="cancelReason"
              rows="3"
              required
              class="field f-textarea"
              :placeholder="t('saleOrderDetail.cancelReasonPlaceholder')"
            />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="cancelModal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="status-btn status-btn-danger" :disabled="statusSaving || !cancelReason.trim()">
              {{ statusSaving ? t('common.saving') : t('saleOrderDetail.cancelSubmit') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="prodModal" class="overlay">
      <div class="modal modal-lg">
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span class="modal-badge"><Factory class="h-5 w-5" /></span>
            <div>
              <h3 class="title">{{ t('saleOrderDetail.sendToProductionTitle', { id: order?.id }) }}</h3>
              <p class="modal-hint mt-1">{{ t('saleOrderDetail.routeBuilderHint') }}</p>
            </div>
          </div>
          <button type="button" class="close-btn" :aria-label="t('common.close')" @click="prodModal = false">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div v-if="prodError" class="err mb-3">{{ prodError }}</div>
        <form class="space-y-4" @submit.prevent="onSendToProduction">
          <div>
            <label class="f-lbl">{{ t('saleOrderDetail.workshopRoute') }} <span class="req">*</span></label>
            <ol v-if="prodRoute.length" class="builder-list">
              <li v-for="(wid, idx) in prodRoute" :key="`${idx}-${wid}`" class="builder-step">
                <span class="route-no">{{ idx + 1 }}</span>
                <span class="flex-1 truncate font-medium">{{ workshopName(wid) }}</span>
                <button type="button" class="icon-btn" :disabled="idx === 0" :aria-label="t('saleOrderDetail.moveUp')" @click="moveRouteStep(idx, -1)">
                  <ArrowUp class="h-4 w-4" />
                </button>
                <button type="button" class="icon-btn" :disabled="idx === prodRoute.length - 1" :aria-label="t('saleOrderDetail.moveDown')" @click="moveRouteStep(idx, 1)">
                  <ArrowDown class="h-4 w-4" />
                </button>
                <button type="button" class="icon-btn danger-icon" :aria-label="t('common.delete')" @click="removeRouteStep(idx)">
                  <Trash2 class="h-4 w-4" />
                </button>
              </li>
            </ol>
            <p v-else class="modal-hint">{{ t('saleOrderDetail.routeEmpty') }}</p>
            <div class="mt-3 flex gap-2">
              <select v-model.number="prodAddWorkshopId" class="field flex-1">
                <option :value="0" disabled>{{ t('saleOrderDetail.selectWorkshop') }}</option>
                <option v-for="w in activeWorkshops" :key="w.id" :value="w.id" :disabled="w.id === prodRoute[prodRoute.length - 1]">
                  {{ w.name }}
                </option>
              </select>
              <button type="button" class="ghost btn-with-icon" :disabled="!prodAddWorkshopId" @click="addRouteStep">
                <Plus class="h-4 w-4" />
                {{ t('saleOrderDetail.addStep') }}
              </button>
            </div>
            <p v-if="routeHasAdjacentDuplicate" class="mt-2 text-xs text-red-600">{{ t('saleOrderDetail.routeDuplicate') }}</p>
          </div>
          <label class="lbl">
            {{ t('common.note') }}
            <input v-model="prodNote" class="field" />
          </label>
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="prodModal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="prodSaving || !prodRoute.length || routeHasAdjacentDuplicate">
              {{ prodSaving ? '...' : t('common.send') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-show="tab === 'waste'" class="card">
      <div class="head">
        <h3 class="title">{{ t('saleOrderDetail.waste') }}</h3>
        <button type="button" class="btn" :disabled="writeBlocked" @click="openWasteCreate">{{ t('saleOrderDetail.addWaste') }}</button>
      </div>
      <table class="min-w-full">
        <thead><tr class="border-b border-gray-100 dark:border-gray-800"><th class="th">{{ t('common.product') }}</th><th class="th">{{ t('saleOrderDetail.quantity') }}</th><th class="th">{{ t('common.comment') }}</th><th class="th text-right">{{ t('common.actions') }}</th></tr></thead>
        <tbody>
          <tr v-if="wastes.length === 0"><td colspan="4" class="empty">{{ t('saleOrderDetail.empty') }}</td></tr>
          <tr v-for="w in wastes" :key="w.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ w.goodsName || w.goodsId }}</td>
            <td class="td">{{ w.quantity }}</td>
            <td class="td">{{ w.comment || '—' }}</td>
            <td class="td text-right"><RowActions :edit="false" @delete="onWasteDelete(w)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="wasteModal" class="overlay">
      <div class="modal">
        <h3 class="title mb-4">{{ t('saleOrderDetail.waste') }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onWasteSave">
          <select v-model.number="wasteForm.goodsId" required class="field">
            <option :value="0" disabled>{{ t('common.product') }}</option>
            <option v-for="g in goods" :key="'w'+g.id" :value="g.id">{{ g.name }}</option>
          </select>
          <input v-model.number="wasteForm.quantity" type="number" min="0.01" step="0.01" required class="field" />
          <input v-model="wasteForm.comment" class="field" :placeholder="t('common.comment')" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="wasteModal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="saving">{{ t('common.save') }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowDown,
  ArrowUp,
  BadgePercent,
  CalendarDays,
  Check,
  CircleDollarSign,
  Factory,
  HandCoins,
  ImagePlus,
  PackageCheck,
  Pencil,
  Plus,
  RefreshCw,
  Scale,
  Trash2,
  Truck,
  UserRound,
  Wallet,
  Warehouse,
  X,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import RowActions from '@/components/crm/RowActions.vue'
import SaleStatusBadge from '@/components/crm/SaleStatusBadge.vue'
import DeliveryStatusBadge from '@/components/crm/DeliveryStatusBadge.vue'
import AuthImage from '@/components/crm/AuthImage.vue'
import { canChangeSaleStatus, SALE_MAIN_FLOW, type SaleStatus } from '@/utils/saleStatus'
import { fetchProductionBySaleOrder, sendToProduction, type ProductionOrder } from '@/api/production'
import { fetchDeliveryBySaleOrder, type Delivery } from '@/api/transport'
import { fetchActiveWorkshops, type Workshop } from '@/api/workshops'
import {
  applyDiscount,
  removeDiscount,
  changeSaleOrderStatus,
  fetchDiscountHistory,
  fetchSaleOrder,
  fetchSaleOrderHistory,
  fetchSaleOrderImages,
  uploadSaleOrderImages,
  deleteSaleOrderImage,
  type SaleOrder,
  type SaleOrderDiscountHistory,
  type SaleOrderHistory,
  type SaleOrderImage,
} from '@/api/sales'
import { fetchSaleOrderItems, type SaleOrderItem } from '@/api/saleOrderItems'
import {
  createSaleOrderWaste,
  deleteSaleOrderWaste,
  fetchSaleOrderWastes,
  type SaleOrderWaste,
} from '@/api/saleOrderWastes'
import { fetchGoods, type Goods } from '@/api/goods'
import { fetchMyDiscountLimit } from '@/api/discountRules'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatDate, formatDateOnly, formatKg, formatOrderDate, money } from '@/utils/format'
import { BASE_CURRENCY, currencySymbol, formatRate, moneyIn, type CurrencyCode } from '@/utils/currency'
import { fetchCurrentRate } from '@/api/exchangeRates'

const { writeBlocked } = useFilialScope()
const { t, te } = useI18n()
const auth = useAuthStore()

const route = useRoute()
const router = useRouter()
const TAB_IDS = ['items', 'discount', 'history', 'images', 'waste'] as const
const tabs = computed(() => TAB_IDS.map((id) => ({ id, label: t(`saleOrderDetail.tabs.${id}`) })))
const tab = ref<(typeof TAB_IDS)[number]>('items')

const order = ref<SaleOrder | null>(null)
const items = ref<SaleOrderItem[]>([])
const discounts = ref<SaleOrderDiscountHistory[]>([])
const history = ref<SaleOrderHistory[]>([])
const images = ref<SaleOrderImage[]>([])
const wastes = ref<SaleOrderWaste[]>([])
const goods = ref<Goods[]>([])
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const saving = ref(false)
const discountType = ref('PERCENTAGE')
const discountValue = ref(10)
const discountComment = ref('')

const orderCurrency = computed<CurrencyCode>(() => order.value?.currency || BASE_CURRENCY)
const isForeign = computed(() => orderCurrency.value !== BASE_CURRENCY)
const curLabel = computed(() => currencySymbol(orderCurrency.value, t('common.currency')))
/** So'mdagi buyurtmada dollardagi mahsulot narxini o'girish uchun bugungi kurs. */
const currentUsdRate = ref(0)
const conversionRate = computed(() => (isForeign.value ? Number(order.value?.exchangeRate || 0) : currentUsdRate.value))

function amt(v?: number | null) {
  return isForeign.value ? moneyIn(v, orderCurrency.value) : money(v)
}

function goodsCurrency(g: Goods): CurrencyCode {
  return g.priceCurrency || BASE_CURRENCY
}

/** Katalog narxi buyurtma valyutasida (backend DiscountLimitPolicy bilan bir xil); kurs bo'lmasa null. */
function listPriceIn(g: Goods) {
  const price = Number(g.priceSelling || 0)
  const from = goodsCurrency(g)
  if (from === orderCurrency.value) return price
  const rate = conversionRate.value
  if (!(rate > 0)) return null
  return Math.round((from === BASE_CURRENCY ? price / rate : price * rate) * 100) / 100
}

async function ensureConversionRate() {
  if (isForeign.value || currentUsdRate.value > 0) return
  if (!goods.value.some((g) => goodsCurrency(g) !== BASE_CURRENCY)) return
  try {
    currentUsdRate.value = Number((await fetchCurrentRate('USD')).data?.rate || 0)
  } catch {
    currentUsdRate.value = 0
  }
}

const canBypassDiscountLimit = computed(() => auth.can('GOODS_EDIT'))

/** Mirrors backend DiscountLimitPolicy: weighted remaining share after item-level price cuts. */
const discountLimit = computed<number | null>(() => {
  const goodsById = new Map(goods.value.map((g) => [g.id, g]))
  let weightSum = 0
  let allowedSum = 0
  let anyLimited = false
  for (const it of items.value) {
    const weight = Number(it.priceSelling || 0) * Number(it.count || 0)
    if (weight <= 0) continue
    const g = it.goodsId != null ? goodsById.get(it.goodsId) : undefined
    let remaining = 1
    if (g?.maxDiscountPercent != null) {
      anyLimited = true
      const limit = Number(g.maxDiscountPercent) / 100
      const list = listPriceIn(g) ?? 0
      const price = Number(it.priceSelling || 0)
      const used = list > 0 && price < list ? (list - price) / list : 0
      remaining = used >= limit ? 0 : 1 - (1 - limit) / (1 - used)
    }
    weightSum += weight
    allowedSum += weight * remaining
  }
  return anyLimited && weightSum > 0 ? allowedSum / weightSum : null
})

const discountBase = computed(() => Number(order.value?.originalTotalSum ?? order.value?.totalSum ?? 0))
const discountLimitAmount = computed(() => discountBase.value * (discountLimit.value ?? 0))

const discountPreview = computed(() => {
  const base = discountBase.value
  const value = Number(discountValue.value || 0)
  if (base <= 0 || value <= 0) return null
  const amount = discountType.value === 'PERCENTAGE' ? (base * value) / 100 : value
  return { amount, fraction: amount / base }
})

/** null - unlimited. Unlike the goods limit, GOODS_EDIT does not bypass it. */
const roleDiscountLimit = ref<number | null>(null)

const discountExceeded = computed(() => {
  if (!discountPreview.value) return false
  const limits = [roleDiscountLimit.value, canBypassDiscountLimit.value ? null : discountLimit.value]
    .filter((v): v is number => v != null)
  if (limits.length === 0) return false
  return discountPreview.value.fraction > Math.min(...limits) + 0.0001
})

async function loadRoleDiscountLimit() {
  try {
    const res = await fetchMyDiscountLimit()
    const pct = res.data?.maxDiscountPercent
    roleDiscountLimit.value = pct != null ? Number(pct) / 100 : null
  } catch {
    roleDiscountLimit.value = null
  }
}

function formatPercent(fraction: number) {
  return `${Math.round(fraction * 10000) / 100}%`
}

function goodsOf(goodsId?: number | null) {
  return goods.value.find((x) => x.id === goodsId)
}

function isWindowGoodsId(goodsId?: number | null) {
  return (goodsOf(goodsId)?.type || '').toUpperCase() === 'WINDOW'
}

function isServiceGoodsId(goodsId?: number | null) {
  return (goodsOf(goodsId)?.type || '').toUpperCase() === 'SERVICE'
}

/** Kesish qatori kategoriya bilan yozilgan: mahsulot nomi emas, kategoriya chiqadi. */
function itemProductLabel(it: SaleOrderItem) {
  if (it.cutCategoryId) {
    const named = goods.value.find((g) => g.categoryId === it.cutCategoryId && g.categoryName)
    if (named?.categoryName) return named.categoryName
  }
  return it.goodsName || String(it.goodsId ?? '')
}

function formatQty(v?: number | null) {
  if (v == null || Number.isNaN(Number(v))) return '—'
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v))
}

function itemPieces(it: SaleOrderItem) {
  if (!isWindowGoodsId(it.goodsId)) return Number(it.count || 0)
  const w = Number(it.width || 0)
  const h = Number(it.height || 0)
  if (!(w > 0 && h > 0)) return Math.round(Number(it.count || 0))
  return Math.max(1, Math.round((Number(it.count || 0) * 10000) / (w * h)))
}

function formatInt(v: number) {
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 0 }).format(Math.round(v || 0))
}

function lineSum(it: SaleOrderItem) {
  return Number(it.count || 0) * Number(it.priceSelling || 0)
}

const itemsTotal = computed(() => items.value.reduce((sum, it) => sum + lineSum(it), 0))
const itemsPieces = computed(() => items.value.reduce((sum, it) => sum + itemPieces(it), 0))
const itemsKvm = computed(() =>
  items.value.reduce((sum, it) => sum + (isWindowGoodsId(it.goodsId) ? Number(it.count || 0) : 0), 0),
)
const itemsWeight = computed(() => items.value.reduce((sum, it) => sum + Number(it.weightKg || 0), 0))
const orderWeight = computed(() => (items.value.length ? itemsWeight.value : Number(order.value?.totalWeightKg || 0)))

const summaryCards = computed(() => {
  const o = order.value
  if (!o) return []
  const discount =
    o.discountType
      ? `${discountTypeLabel(o.discountType)}${o.discountValue != null ? ` ${o.discountValue}${o.discountType === 'PERCENTAGE' ? '%' : ''}` : ''}`
      : '—'
  return [
    {
      key: 'total',
      label: t('common.total'),
      value: amt(o.totalSum),
      hint: isForeign.value && o.totalSumBase != null
        ? `≈ ${moneyIn(o.totalSumBase, 'UZS', t('common.currency'))}`
        : undefined,
      icon: Wallet,
      tone: 'tone-brand',
    },
    {
      key: 'paid',
      label: t('saleOrderDetail.paid'),
      value: amt(o.paidSum),
      icon: HandCoins,
      tone: 'tone-ok',
    },
    {
      key: 'discount',
      label: t('saleOrderDetail.discount'),
      value: discount,
      icon: BadgePercent,
      tone: 'tone-violet',
    },
    {
      key: 'debt',
      label: t('saleOrderDetail.debt'),
      value: amt(o.debtSum),
      icon: CircleDollarSign,
      tone: 'tone-warn',
    },
    {
      key: 'status',
      label: t('common.status'),
      value: statusLabel(o.orderStatus || ''),
      icon: PackageCheck,
      tone: 'tone-slate',
    },
    {
      key: 'date',
      label: t('common.date'),
      value: formatOrderDate(o.orderDate, o.createdAt),
      icon: CalendarDays,
      tone: 'tone-slate',
    },
  ]
})

const wasteModal = ref(false)
const wasteForm = reactive({ goodsId: 0, quantity: 1, comment: '' })
const statusSaving = ref(false)
const cancelModal = ref(false)
const cancelReason = ref('')
const cancelError = ref<string | null>(null)
const prodModal = ref(false)
const prodRoute = ref<number[]>([])
const prodAddWorkshopId = ref(0)
const production = ref<ProductionOrder | null>(null)
const routeHasAdjacentDuplicate = computed(() =>
  prodRoute.value.some((wid, i) => i > 0 && prodRoute.value[i - 1] === wid),
)

function workshopName(wid: number) {
  return activeWorkshops.value.find((w) => w.id === wid)?.name || `#${wid}`
}

function addRouteStep() {
  if (!prodAddWorkshopId.value) return
  prodRoute.value.push(prodAddWorkshopId.value)
  prodAddWorkshopId.value = 0
}

function moveRouteStep(idx: number, dir: -1 | 1) {
  const target = idx + dir
  if (target < 0 || target >= prodRoute.value.length) return
  const next = [...prodRoute.value]
  ;[next[idx], next[target]] = [next[target], next[idx]]
  prodRoute.value = next
}

function removeRouteStep(idx: number) {
  prodRoute.value.splice(idx, 1)
}
const prodNote = ref('')
const prodError = ref<string | null>(null)
const prodSaving = ref(false)
const activeWorkshops = ref<Workshop[]>([])
const uploading = ref(false)
const fileInputKey = ref(0)
const preview = ref<SaleOrderImage | null>(null)

const delivery = ref<Delivery | null>(null)

const flowStatuses = computed<SaleStatus[]>(() => {
  const status = order.value?.orderStatus
  const selfPickup =
    !delivery.value &&
    (status === 'DELIVERED' || status === 'WORK_DONE' || status === 'COMPLETED' || order.value?.deliveryType === 'PICKUP')
  const base = selfPickup ? SALE_MAIN_FLOW.filter((s) => s !== 'IN_DELIVERY') : [...SALE_MAIN_FLOW]
  // Bekor qilish har doim oxirida (yoki allaqachon bekor qilingan)
  if (status === 'CANCELLED' || !isFinalSaleStatusLocal(status)) {
    return [...base, 'CANCELLED']
  }
  return base
})
const deliveryConfirmBlocked = computed(
  () =>
    order.value?.orderStatus === 'IN_DELIVERY' &&
    !(delivery.value && ['IN_TRANSIT', 'ARRIVED'].includes(delivery.value.deliveryStatus)),
)
/** Backend bilan bir xil: 0.01 dan kichik qoldiq (kurs yaxlitlashi) to'langan hisoblanadi. */
const completeBlocked = computed(() => Number(order.value?.debtSum ?? 0) >= 0.01)
const flowIndex = computed(() => flowStatuses.value.indexOf(order.value?.orderStatus as SaleStatus))
const isFinalOrder = computed(() => ['COMPLETED', 'CANCELLED'].includes(order.value?.orderStatus || ''))
const canDiscountDebt = computed(() => !isFinalOrder.value && Number(order.value?.debtSum || 0) >= 0.01)
const doneIndex = computed(() => (order.value?.orderStatus === 'CANCELLED' ? -1 : flowIndex.value))

const statusReachedMap = computed(() => {
  const map = new Map<string, string>()
  // NEW ("Yaratilgan") — buyurtma yaratilgan sana/vaqt (history bo‘lmasa ham)
  if (order.value?.orderDate || order.value?.createdAt) {
    map.set('NEW', order.value?.createdAt || order.value?.orderDate || '')
  }
  for (const h of history.value) {
    if (!h.toStatus || !h.changedAt) continue
    // Eng so‘nggi o‘tish vaqti (orqaga qaytganda ham yangilanadi)
    map.set(h.toStatus, h.changedAt)
  }
  return map
})

function statusReachedAt(st: SaleStatus) {
  if (st === 'NEW') {
    return formatOrderDate(order.value?.orderDate, order.value?.createdAt)
  }
  const at = statusReachedMap.value.get(st)
  return at ? formatDate(at) : '—'
}

function dayDiff(from?: string | null, to?: string | null) {
  if (!from || !to) return null
  const a = Date.parse(`${from.slice(0, 10)}T00:00:00`)
  const b = Date.parse(`${to.slice(0, 10)}T00:00:00`)
  if (!Number.isFinite(a) || !Number.isFinite(b)) return null
  return Math.round((b - a) / 86_400_000)
}

const dueDateRaw = computed(() => order.value?.plannedReadyDate || null)

const completedAt = computed(() => {
  if (order.value?.orderStatus !== 'COMPLETED') return null
  return statusReachedMap.value.get('COMPLETED') || null
})

/** Yakunlanganda muddat bilan solishtirish; yakunlanmaganda — hozirgi kechikish. */
const deadlineDiffDays = computed(() => {
  const due = dueDateRaw.value
  if (!due) return null
  if (completedAt.value) return dayDiff(due, completedAt.value)
  if (order.value?.orderStatus === 'CANCELLED') return null
  const today = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const now = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
  return dayDiff(due, now)
})

const deadlineCompareText = computed(() => {
  const d = deadlineDiffDays.value
  if (d == null || !dueDateRaw.value) return ''
  if (!completedAt.value) return ''
  if (d > 0) return t('saleOrderDetail.daysLate', { n: d })
  if (d < 0) return t('saleOrderDetail.daysEarly', { n: Math.abs(d) })
  return t('saleOrderDetail.onTime')
})

const deadlineCompareTone = computed(() => {
  const d = deadlineDiffDays.value
  if (d == null) return ''
  if (d > 0) return 'is-late'
  if (d < 0) return 'is-early'
  return 'is-ontime'
})

const deadlineBanner = computed(() => {
  const due = dueDateRaw.value
  if (!due) return null
  const dueLabel = formatDateOnly(due)
  const d = deadlineDiffDays.value
  if (completedAt.value) {
    return {
      tone: d != null && d > 0 ? 'danger' : d != null && d < 0 ? 'ok' : 'neutral',
      text: `${dueLabel}${deadlineCompareText.value ? ` · ${deadlineCompareText.value}` : ''}`,
    }
  }
  if (d != null && d > 0) {
    return {
      tone: 'danger',
      text: `${dueLabel} · ${t('saleOrderDetail.overdueBy', { n: d })}`,
    }
  }
  return {
    tone: 'neutral',
    text: dueLabel,
  }
})

function isFinalSaleStatusLocal(status?: string | null) {
  return status === 'COMPLETED' || status === 'CANCELLED'
}

function canClickStatus(st: SaleStatus) {
  const current = order.value?.orderStatus
  if (!current || statusSaving.value || writeBlocked.value) return false
  if (st === current) return false
  if (!canChangeSaleStatus(current, st)) return false
  if (st === 'IN_DELIVERY' && order.value?.deliveryType === 'PICKUP') return false
  if (st === 'DELIVERED' && deliveryConfirmBlocked.value) return false
  if (st === 'COMPLETED' && completeBlocked.value) return false
  return true
}

function statusLabel(status: string) {
  const key = `saleStatus.${status}`
  return te(key) ? t(key) : status
}

function discountTypeLabel(type: string) {
  const key = `saleOrderDetail.discountTypes.${type}`
  return te(key) ? t(key) : type
}

function statusButtonLabel(st: SaleStatus) {
  if (st === 'PROCESSING') return t('saleOrderDetail.sendToProduction')
  if (st === 'IN_DELIVERY') return t('saleOrderDetail.sendToDelivery')
  if (st === 'DELIVERED' && order.value?.orderStatus === 'IN_DELIVERY') return t('saleOrderDetail.confirmDelivered')
  return t('saleOrderDetail.moveTo', { status: statusLabel(st) })
}

async function loadDelivery() {
  try {
    delivery.value = (await fetchDeliveryBySaleOrder(id())).data ?? null
  } catch {
    delivery.value = null
  }
}

function imageSrc(img: SaleOrderImage) {
  return img.downloadUrl || (img.fileName ? `/api/files/${img.fileName}` : '')
}

function openPreview(img: SaleOrderImage) {
  preview.value = img
}

function id() {
  return Number(route.params.id)
}

async function load() {
  error.value = null
  try {
    const [o, its, disc, hist, imgs, w, gs] = await Promise.all([
      fetchSaleOrder(id()),
      fetchSaleOrderItems(id()),
      fetchDiscountHistory(id()),
      fetchSaleOrderHistory(id()),
      fetchSaleOrderImages(id()),
      fetchSaleOrderWastes(id()),
      fetchGoods(),
    ])
    order.value = o.data
    items.value = its.data || []
    discounts.value = disc.data || []
    history.value = hist.data || []
    images.value = imgs.data || []
    wastes.value = w.data || []
    goods.value = gs.data || []
    await Promise.all([loadDelivery(), loadProduction(), ensureConversionRate()])
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  }
}

const refreshing = ref(false)

async function onRefresh() {
  refreshing.value = true
  try {
    await load()
  } finally {
    refreshing.value = false
  }
}

function openEdit() {
  if (!order.value) return
  void router.push(`/sales/${order.value.id}/edit`)
}

/** Chegirma almashtiriladi (qo'shilmaydi): yangi summa = joriy chegirma + qolgan qarz, shunda qarz 0 bo'ladi. */
function fillDebtDiscount() {
  if (!order.value) return
  const debt = Number(order.value.debtSum || 0)
  const current = Number(order.value.discountAmount || 0)
  discountType.value = 'FIXED_AMOUNT'
  discountValue.value = Math.round((current + debt) * 100) / 100
  if (!discountComment.value.trim()) discountComment.value = t('saleOrderDetail.discountDebtComment')
}

async function onDiscount() {
  saving.value = true
  try {
    await applyDiscount(id(), discountType.value, discountValue.value, discountComment.value)
    discountComment.value = ''
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onRemoveDiscount() {
  if (!order.value) return
  if (!confirm(t('saleOrderDetail.revertDiscountConfirm', { amount: amt(order.value.discountAmount) }))) return
  saving.value = true
  error.value = null
  try {
    await removeDiscount(id(), discountComment.value)
    discountComment.value = ''
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function reloadImages() {
  images.value = (await fetchSaleOrderImages(id())).data || []
}

async function onUploadImages(e: Event) {
  const files = Array.from((e.target as HTMLInputElement).files || [])
  if (!files.length) return
  uploading.value = true
  error.value = null
  try {
    await uploadSaleOrderImages(id(), files)
    await reloadImages()
  } catch (err) {
    error.value = formatApiError(err, t('saleOrderDetail.uploadError'))
  } finally {
    uploading.value = false
    fileInputKey.value += 1
  }
}

async function onDeleteImage(img: SaleOrderImage) {
  if (!confirm(t('saleOrderDetail.deleteImageConfirm'))) return
  try {
    await deleteSaleOrderImage(id(), img.id)
    if (preview.value?.id === img.id) preview.value = null
    await reloadImages()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onStatus(status: SaleStatus) {
  if (!order.value) return
  if (status === 'PROCESSING') {
    await openProdModal()
    return
  }
  if (status === 'CANCELLED') {
    cancelReason.value = ''
    cancelError.value = null
    cancelModal.value = true
    return
  }
  if (status === 'IN_DELIVERY' && !confirm(t('saleOrderDetail.deliveryConfirm', { id: order.value.id }))) return
  if (
    status === 'DELIVERED' &&
    order.value.orderStatus === 'IN_DELIVERY' &&
    !confirm(t('saleOrderDetail.deliveredConfirm'))
  )
    return
  statusSaving.value = true
  error.value = null
  try {
    await changeSaleOrderStatus(order.value.id, status)
    await load()
  } catch (e) {
    error.value = formatApiError(e, t('saleOrderDetail.statusError'))
  } finally {
    statusSaving.value = false
  }
}

async function onCancelConfirm() {
  if (!order.value) return
  const reason = cancelReason.value.trim()
  if (!reason) {
    cancelError.value = t('saleOrderDetail.cancelReasonRequired')
    return
  }
  statusSaving.value = true
  cancelError.value = null
  try {
    await changeSaleOrderStatus(order.value.id, 'CANCELLED', reason)
    cancelModal.value = false
    await load()
  } catch (e) {
    cancelError.value = formatApiError(e, t('saleOrderDetail.statusError'))
  } finally {
    statusSaving.value = false
  }
}

async function openProdModal() {
  prodRoute.value = []
  prodAddWorkshopId.value = 0
  prodNote.value = ''
  prodError.value = null
  prodModal.value = true
  try {
    activeWorkshops.value = (await fetchActiveWorkshops()).data || []
  } catch (e) {
    prodError.value = formatApiError(e, t('saleOrderDetail.workshopsLoadError'))
  }
}

async function loadProduction() {
  try {
    production.value = (await fetchProductionBySaleOrder(id())).data ?? null
  } catch {
    production.value = null
  }
}

async function onSendToProduction() {
  if (!order.value || !prodRoute.value.length || routeHasAdjacentDuplicate.value) return
  prodSaving.value = true
  prodError.value = null
  try {
    await sendToProduction({
      saleOrderId: order.value.id,
      workshopIds: [...prodRoute.value],
      note: prodNote.value.trim() || undefined,
    })
    prodModal.value = false
    await load()
  } catch (e) {
    prodError.value = formatApiError(e, t('saleOrderDetail.sendError'))
  } finally {
    prodSaving.value = false
  }
}

function openWasteCreate() {
  wasteForm.goodsId = goods.value[0]?.id || 0
  wasteForm.quantity = 1
  wasteForm.comment = ''
  formError.value = null
  wasteModal.value = true
}

async function onWasteSave() {
  saving.value = true
  formError.value = null
  try {
    await createSaleOrderWaste({
      saleOrderId: id(),
      goodsId: wasteForm.goodsId,
      quantity: wasteForm.quantity,
      comment: wasteForm.comment || undefined,
    })
    wasteModal.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onWasteDelete(w: SaleOrderWaste) {
  if (!confirm(t('saleOrderDetail.deleteWasteConfirm'))) return
  try {
    await deleteSaleOrderWaste(w.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(() => {
  load()
  loadRoleDiscountLimit()
})

watch(
  () => route.params.id,
  (next, prev) => {
    if (next && next !== prev) {
      preview.value = null
      load()
    }
  },
)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.head { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 0.75rem; padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.meta-chip {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 0.35rem;
  border-radius: 9999px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  padding: 0.25rem 0.7rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #374151;
}
.info-card {
  display: flex;
  align-items: flex-start;
  gap: 0.7rem;
  border-radius: 0.9rem;
  border: 1px solid #eef0f3;
  background: #fff;
  padding: 0.85rem 0.95rem;
}
.info-icon {
  display: inline-flex;
  height: 2rem;
  width: 2rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 0.55rem;
}
.tone-brand { background: #eef2ff; color: #465fff; }
.tone-warn { background: #fff6ed; color: #ec4a0a; }
.tone-ok { background: #ecfdf3; color: #039855; }
.tone-violet { background: #f4f3ff; color: #6938ef; }
.tone-slate { background: #f2f4f7; color: #475467; }
.info-lbl { font-size: 0.72rem; font-weight: 500; color: #98a2b3; }
.info-val { margin-top: 0.15rem; font-size: 0.95rem; font-weight: 650; color: #1f2937; line-height: 1.25; word-break: break-word; }
.info-hint { margin-top: 0.15rem; font-size: 0.7rem; color: #98a2b3; }
.items-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem 1.25rem;
  border-bottom: 1px solid #f3f4f6;
  padding: 0.7rem 1.25rem;
  font-size: 0.8125rem;
  color: #475467;
}
.items-meta span { display: inline-flex; align-items: center; gap: 0.35rem; }
.items-meta b { font-weight: 600; color: #1f2937; }
.sum-row.soft .td { font-weight: 500; color: #475467; }
.meta-chip-link {
  text-decoration: none;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}
.meta-chip-link:hover {
  border-color: #98a2b3;
  color: #465fff;
  background: #eef2ff;
}
.dark .meta-chip { border-color: #344054; background: rgb(255 255 255 / 4%); color: #d0d5dd; }
.dark .meta-chip-link:hover {
  border-color: #475467;
  color: #8098f9;
  background: rgb(70 95 255 / 12%);
}
.dark .info-card { border-color: #1f2937; background: rgb(255 255 255 / 3%); }
.dark .info-lbl, .dark .info-hint { color: #667085; }
.dark .info-val { color: rgba(255, 255, 255, 0.92); }
.dark .tone-brand { background: rgb(70 95 255 / 18%); color: #8098f9; }
.dark .tone-warn { background: rgb(236 74 10 / 16%); color: #fd853a; }
.dark .tone-ok { background: rgb(3 152 85 / 16%); color: #32d583; }
.dark .tone-violet { background: rgb(105 56 239 / 18%); color: #9b8afb; }
.dark .tone-slate { background: rgb(255 255 255 / 6%); color: #98a2b3; }
.dark .items-meta { border-color: #1f2937; color: #98a2b3; }
.dark .items-meta b { color: rgba(255, 255, 255, 0.9); }
.dark .sum-row.soft .td { color: #98a2b3; }
.kind-badge { display: inline-flex; border-radius: 9999px; padding: 0.125rem 0.6rem; font-size: 0.75rem; font-weight: 500; white-space: nowrap; }
.kind-badge.product { background: #eef2ff; color: #465fff; }
.kind-badge.service { background: #fff7ed; color: #c2410c; }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.limit-card { border-radius: 0.5rem; border: 1px solid #fde68a; background: #fffbeb; padding: 0.65rem 0.9rem; font-size: 0.875rem; color: #92400e; }
.dark .limit-card { border-color: rgba(245, 158, 11, 0.3); background: rgba(245, 158, 11, 0.1); color: #fcd34d; }
.btn-debt { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #fdb022; background: #fffaeb; padding: 0 0.9rem; font-size: 0.8125rem; font-weight: 500; color: #b54708; }
.btn-debt:hover:not(:disabled) { background: #fef0c7; }
.btn-debt:disabled { opacity: 0.5; cursor: not-allowed; }
.field-area { width: 100%; border-radius: 0.5rem; border: 1px solid #d0d5dd; background: transparent; padding: 0.5rem 0.75rem; font-size: 0.875rem; }
.dark .btn-debt { border-color: rgb(247 144 9 / 45%); background: rgb(247 144 9 / 10%); color: #fdb022; }
.dark .field-area { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.current-discount { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.75rem; border-radius: 0.5rem; border: 1px solid #e4e7ec; padding: 0.65rem 0.9rem; }
.btn-revert { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #fda29b; padding: 0 0.9rem; font-size: 0.8125rem; font-weight: 500; color: #d92d20; }
.btn-revert:hover:not(:disabled) { background: #fef3f2; }
.btn-revert:disabled { opacity: 0.5; cursor: not-allowed; }
.dark .current-discount { border-color: #344054; }
.dark .btn-revert { border-color: rgb(240 68 56 / 45%); color: #f97066; }
.dark .btn-revert:hover:not(:disabled) { background: rgb(240 68 56 / 10%); }
.lbl { display: block; margin-bottom: 0.25rem; font-size: 0.75rem; color: #6b7280; }
.tab {
  display: inline-flex;
  height: 2.25rem;
  align-items: center;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 1rem;
  font-size: 0.875rem;
  color: inherit;
  text-decoration: none;
}
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }
.tab:hover:not(.active) { border-color: #98a2b3; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
.modal-lbl { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.modal-hint { font-size: 0.75rem; color: #6b7280; }
.base-eq { display: block; font-size: 0.75rem; color: #6b7280; }
.dark .base-eq { color: #9ca3af; }
.dark .modal-lbl, .dark .modal-hint { color: #9ca3af; }
.sub { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.status-head { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: flex-start; gap: 1rem; }
.status-click-hint { margin-top: 0.2rem; font-size: 0.7rem; color: #98a2b3; }
.deadline-banner {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.15rem;
  border-radius: 0.65rem;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  padding: 0.45rem 0.75rem;
  text-align: right;
}
.deadline-banner-lbl { font-size: 0.68rem; font-weight: 500; color: #98a2b3; }
.deadline-banner-val { font-size: 0.8125rem; font-weight: 650; color: #344054; }
.deadline-banner.danger {
  border-color: #fda29b;
  background: #fef3f2;
}
.deadline-banner.danger .deadline-banner-lbl,
.deadline-banner.danger .deadline-banner-val { color: #d92d20; }
.deadline-banner.ok {
  border-color: #a6f4c5;
  background: #ecfdf3;
}
.deadline-banner.ok .deadline-banner-lbl,
.deadline-banner.ok .deadline-banner-val { color: #039855; }
.status-flow { display: flex; flex-wrap: wrap; align-items: stretch; gap: 0.45rem; margin-top: 1rem; }
.flow-step {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.1rem;
  border-radius: 0.7rem;
  border: 1px solid #e5e7eb;
  padding: 0.4rem 0.7rem;
  font-size: 0.75rem;
  color: #6b7280;
  background: #f9fafb;
  text-align: left;
  line-height: 1.2;
}
.flow-step-label { font-weight: 600; }
.flow-step-date { font-size: 0.68rem; opacity: 0.85; white-space: nowrap; }
.flow-step-meta { font-size: 0.68rem; font-weight: 600; }
.flow-step-meta.is-late { color: #d92d20; }
.flow-step-meta.is-early { color: #039855; }
.flow-step-meta.is-ontime { color: #465fff; }
.flow-step.done { border-color: #c7d2fe; background: #eef2ff; color: #4338ca; }
.flow-step.current { border-color: #465fff; background: #465fff; color: #fff; font-weight: 600; }
.flow-step.current .flow-step-meta.is-late { color: #fecaca; }
.flow-step.current .flow-step-meta.is-early,
.flow-step.current .flow-step-meta.is-ontime { color: #d1fadf; }
.flow-step.current.cancelled { border-color: #dc2626; background: #dc2626; }
.flow-step.clickable { cursor: pointer; }
.flow-step.clickable:hover:not(:disabled) { border-color: #465fff; box-shadow: 0 0 0 2px rgb(70 95 255 / 15%); }
.flow-step.cancelled:not(.current) { border-color: #fda29b; color: #d92d20; background: #fef3f2; }
.flow-step:disabled:not(.current):not(.done) { opacity: 0.7; cursor: default; }
.flow-sep { align-self: center; height: 2px; width: 0.85rem; background: #e5e7eb; flex-shrink: 0; }
.flow-sep.done { background: #465fff; }
.status-btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; }
.status-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.status-btn-primary { background: #465fff; color: #fff; }
.status-btn-primary:hover:not(:disabled) { background: #3641f5; }
.status-btn-danger { border: 1px solid #fecaca; background: #fef2f2; color: #dc2626; }
.status-btn-danger:hover:not(:disabled) { background: #fee2e2; }
.image-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(9.5rem, 1fr)); gap: 0.75rem; }
.image-card { overflow: hidden; border-radius: 0.75rem; border: 1px solid #e5e7eb; background: #fff; }
.image-open { display: block; width: 100%; aspect-ratio: 1 / 1; overflow: hidden; }
.image-thumb { height: 100%; width: 100%; object-fit: cover; transition: transform 0.2s; }
.image-open:hover .image-thumb { transform: scale(1.04); }
.image-meta { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; padding: 0.4rem 0.6rem; font-size: 0.75rem; color: #4b5563; }
.image-delete { display: inline-flex; flex-shrink: 0; height: 1.75rem; width: 1.75rem; align-items: center; justify-content: center; border-radius: 0.375rem; color: #dc2626; }
.image-delete:hover { background: #fef2f2; }
.preview-box { position: relative; max-width: min(90vw, 1100px); border-radius: 1rem; background: #111827; padding: 0.75rem; }
.preview-img { display: block; max-height: 80vh; max-width: 100%; margin: 0 auto; border-radius: 0.5rem; object-fit: contain; min-height: 12rem; min-width: 12rem; }
.preview-name { margin-top: 0.5rem; text-align: center; font-size: 0.8125rem; color: #d1d5db; }
.preview-close { position: absolute; top: -0.75rem; right: -0.75rem; display: inline-flex; height: 2rem; width: 2rem; align-items: center; justify-content: center; border-radius: 9999px; background: #fff; color: #111827; box-shadow: 0 2px 8px rgb(0 0 0 / 25%); }
.dark .sub { color: #9ca3af; }
.dark .status-click-hint { color: #667085; }
.dark .deadline-banner { border-color: #344054; background: rgb(255 255 255 / 4%); }
.dark .deadline-banner-val { color: rgba(255, 255, 255, 0.9); }
.dark .deadline-banner.danger { border-color: rgb(240 68 56 / 45%); background: rgb(240 68 56 / 12%); }
.dark .deadline-banner.ok { border-color: rgb(3 152 85 / 35%); background: rgb(3 152 85 / 12%); }
.dark .flow-step { border-color: #374151; background: #1f2937; color: #9ca3af; }
.dark .flow-step.done { border-color: rgb(70 95 255 / 45%); background: rgb(70 95 255 / 15%); color: #a5b4fc; }
.dark .flow-step.current { border-color: #465fff; background: #465fff; color: #fff; }
.dark .flow-step.current.cancelled { border-color: #dc2626; background: #dc2626; }
.dark .flow-step.cancelled:not(.current) { border-color: rgb(240 68 56 / 45%); background: rgb(240 68 56 / 12%); color: #f97066; }
.dark .flow-step-meta.is-late { color: #f97066; }
.dark .flow-step-meta.is-early { color: #32d583; }
.dark .flow-sep { background: #374151; }
.dark .flow-sep.done { background: #465fff; }
.dark .status-btn-danger { border-color: rgb(248 113 113 / 40%); background: rgb(127 29 29 / 30%); color: #fca5a5; }
.dark .status-btn-danger:hover:not(:disabled) { background: rgb(127 29 29 / 50%); }
.dark .image-card { border-color: #1f2937; background: #111827; }
.dark .image-meta { color: #d1d5db; }
.dark .image-delete { color: #f87171; }
.dark .image-delete:hover { background: rgb(127 29 29 / 30%); }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-with-icon { gap: 0.4rem; }
.ghost:disabled { opacity: 0.6; cursor: wait; }
.sum-row .td { border-top: 1px solid #e5e7eb; font-weight: 700; color: #1f2937; }
.dark .sum-row .td { border-color: #1f2937; color: rgba(255, 255, 255, 0.9); }
.modal-lg { max-width: 36rem; max-height: calc(100vh - 2rem); overflow-y: auto; }
.modal-badge { display: flex; height: 2.5rem; width: 2.5rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.75rem; background: #eff4ff; color: #465fff; }
.close-btn { display: flex; height: 2.25rem; width: 2.25rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; color: #9ca3af; }
.close-btn:hover { background: #f3f4f6; color: #374151; }
.f-lbl { display: block; margin-bottom: 0.375rem; font-size: 0.875rem; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.dark .modal-badge { background: rgb(70 95 255 / 12%); color: #9cb0ff; }
.dark .close-btn:hover { background: rgb(255 255 255 / 5%); color: rgba(255, 255, 255, 0.8); }
.dark .f-lbl { color: #9ca3af; }
.f-textarea { height: auto; min-height: 4.5rem; padding-top: 0.6rem; padding-bottom: 0.6rem; resize: vertical; line-height: 1.4; }
.route-list { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }
.route-step { display: inline-flex; align-items: center; gap: 0.5rem; border-radius: 9999px; border: 1px solid #e5e7eb; background: #f9fafb; padding: 0.35rem 0.85rem 0.35rem 0.35rem; font-size: 0.875rem; color: #374151; }
.route-step + .route-step::before { content: '→'; margin-left: -0.25rem; margin-right: 0.25rem; color: #98a2b3; }
.route-no { display: inline-flex; height: 1.6rem; width: 1.6rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 9999px; background: #e5e7eb; font-size: 0.75rem; font-weight: 700; color: #475467; }
.route-name { font-weight: 600; }
.route-state { font-size: 0.7rem; color: #6b7280; }
.route-step.done { border-color: #a6f4c5; background: #ecfdf3; }
.route-step.done .route-no { background: #12b76a; color: #fff; }
.route-step.current { border-color: #9cb0ff; background: #eff4ff; }
.route-step.current .route-no { background: #465fff; color: #fff; }
.dark .route-step { border-color: #344054; background: rgb(255 255 255 / 3%); color: rgba(255, 255, 255, 0.85); }
.dark .route-no { background: #344054; color: #d0d5dd; }
.dark .route-state { color: #9ca3af; }
.dark .route-step.done { border-color: rgb(18 183 106 / 40%); background: rgb(18 183 106 / 10%); }
.dark .route-step.current { border-color: rgb(70 95 255 / 50%); background: rgb(70 95 255 / 12%); }
.builder-list { display: flex; flex-direction: column; gap: 0.5rem; }
.builder-step { display: flex; align-items: center; gap: 0.6rem; border-radius: 0.6rem; border: 1px solid #e5e7eb; padding: 0.45rem 0.5rem 0.45rem 0.6rem; font-size: 0.875rem; color: #1f2937; }
.builder-step .route-no { background: #465fff; color: #fff; }
.icon-btn { display: inline-flex; height: 2rem; width: 2rem; align-items: center; justify-content: center; border-radius: 0.5rem; color: #6b7280; }
.icon-btn:hover:not(:disabled) { background: #f3f4f6; color: #1f2937; }
.icon-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.icon-btn.danger-icon:hover:not(:disabled) { background: #fef2f2; color: #dc2626; }
.dark .builder-step { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.dark .icon-btn { color: #9ca3af; }
.dark .icon-btn:hover:not(:disabled) { background: rgb(255 255 255 / 5%); color: #fff; }
.dark .icon-btn.danger-icon:hover:not(:disabled) { background: rgb(127 29 29 / 30%); color: #f87171; }
</style>
