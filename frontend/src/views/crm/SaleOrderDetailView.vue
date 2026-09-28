<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="order ? t('saleOrderDetail.title', { id: order.id }) : t('saleOrderDetail.titleFallback')" />
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <router-link to="/sales" class="ghost">{{ t('common.backToList') }}</router-link>
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
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="order" class="card mb-4 p-5">
      <div class="grid grid-cols-2 gap-3 text-sm md:grid-cols-4">
        <div><span class="lbl">{{ t('common.client') }}</span>{{ order.clientFullName || '—' }}</div>
        <div><span class="lbl">{{ t('common.warehouse') }}</span>{{ order.warehouseName || '—' }}</div>
        <div><span class="lbl">{{ t('common.total') }}</span>{{ money(order.totalSum) }}</div>
        <div><span class="lbl">{{ t('saleOrderDetail.debt') }}</span>{{ money(order.debtSum) }}</div>
        <div><span class="lbl">{{ t('saleOrderDetail.paid') }}</span>{{ money(order.paidSum) }}</div>
        <div><span class="lbl">{{ t('saleOrderDetail.discount') }}</span>{{ order.discountType ? discountTypeLabel(order.discountType) : '—' }} {{ order.discountValue || '' }}</div>
        <div><span class="lbl">{{ t('common.status') }}</span><SaleStatusBadge :status="order.orderStatus" /></div>
        <div><span class="lbl">{{ t('common.date') }}</span>{{ formatDate(order.orderDate) }}</div>
        <div>
          <span class="lbl">{{ t('saleOrderCreate.deliveryTitle') }}</span>
          <span v-if="order.deliveryType" class="inline-flex items-center gap-1.5">
            <Truck v-if="order.deliveryType === 'DELIVERY'" class="h-4 w-4 text-brand-500" />
            <PackageCheck v-else class="h-4 w-4 text-success-500" />
            {{ order.deliveryType === 'DELIVERY' ? t('saleOrderCreate.delivery') : t('saleOrderCreate.pickup') }}
          </span>
          <span v-else>—</span>
        </div>
        <div v-if="order.deliveryType === 'DELIVERY'">
          <span class="lbl">{{ t('saleOrderCreate.deliveryFee') }}</span>{{ money(order.deliveryFee || 0) }}
        </div>
        <div v-if="order.comment" class="col-span-2 md:col-span-4">
          <span class="lbl">{{ t('common.comment') }}</span><span class="whitespace-pre-line">{{ order.comment }}</span>
        </div>
      </div>
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

    <div v-if="order" class="card mb-4 p-5">
      <div class="status-head">
        <div>
          <h3 class="title">{{ t('saleOrderDetail.changeStatus') }}</h3>
          <p class="sub">{{ t('saleOrderDetail.currentStatus') }} <SaleStatusBadge :status="order.orderStatus" /></p>
        </div>
      </div>
      <div class="status-flow">
        <template v-for="(st, i) in flowStatuses" :key="st">
          <span v-if="i > 0" class="flow-sep" :class="{ done: doneIndex >= i }" />
          <span class="flow-step" :class="{ done: doneIndex >= i, current: flowIndex === i, cancelled: st === 'CANCELLED' }">
            {{ statusLabel(st) }}
          </span>
        </template>
      </div>
      <div v-if="nextStatuses.length" class="mt-4 flex flex-wrap gap-2">
        <button
          v-for="st in nextStatuses"
          :key="st"
          type="button"
          class="status-btn"
          :class="st === 'CANCELLED' ? 'status-btn-danger' : 'status-btn-primary'"
          :disabled="statusSaving || writeBlocked || (st === 'DELIVERED' && deliveryConfirmBlocked)"
          @click="onStatus(st)"
        >
          {{ statusButtonLabel(st) }}
        </button>
      </div>
      <p v-else class="mt-4 text-sm text-gray-500 dark:text-gray-400">{{ t('saleOrderDetail.noNextStatus') }}</p>
      <p v-if="order.orderStatus === 'PROCESSING'" class="mt-2 text-xs text-gray-500 dark:text-gray-400">
        {{ t('saleOrderDetail.processingHint', { status: statusLabel('READY') }) }}
      </p>
      <p v-if="order.orderStatus === 'READY'" class="mt-2 text-xs text-gray-500 dark:text-gray-400">
        {{ t('saleOrderDetail.readyHint', { inDelivery: statusLabel('IN_DELIVERY'), delivered: statusLabel('DELIVERED') }) }}
      </p>
      <p v-if="order.orderStatus === 'IN_DELIVERY' && deliveryConfirmBlocked" class="mt-2 text-xs text-amber-600 dark:text-amber-400">
        {{ t('saleOrderDetail.deliveryConfirmBlocked') }}
      </p>
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

    <div class="mb-4 flex flex-wrap gap-2">
      <button v-for="tb in tabs" :key="tb.id" type="button" class="tab" :class="{ active: tab === tb.id }" @click="tab = tb.id">{{ tb.label }}</button>
    </div>

    <div v-show="tab === 'items'" class="card">
      <div class="head">
        <h3 class="title">{{ t('saleOrderDetail.tabs.items') }}</h3>
        <button type="button" class="btn" :disabled="writeBlocked" @click="openItemCreate">{{ t('saleOrderDetail.addItem') }}</button>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">{{ t('common.product') }}</th><th class="th">{{ t('common.count') }}</th><th class="th">{{ t('saleOrderDetail.selling') }}</th><th class="th">{{ t('common.date') }}</th><th class="th text-right">{{ t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="items.length === 0"><td colspan="5" class="empty">{{ t('saleOrderDetail.noItems') }}</td></tr>
          <tr v-for="it in items" :key="it.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ it.goodsName || it.goodsId }}</td>
            <td class="td">{{ itemQtyText(it) }}</td>
            <td class="td">{{ money(it.priceSelling) }}</td>
            <td class="td">{{ formatDate(it.arrivalDate) }}</td>
            <td class="td text-right"><RowActions @edit="openItemEdit(it)" @delete="onItemDelete(it)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'discount'" class="card p-5 space-y-4">
      <form class="flex flex-wrap items-end gap-2" @submit.prevent="onDiscount">
        <div>
          <label class="lbl">{{ t('common.type') }}</label>
          <select v-model="discountType" class="field w-40">
            <option value="PERCENTAGE">{{ discountTypeLabel('PERCENTAGE') }}</option>
            <option value="FIXED_AMOUNT">{{ discountTypeLabel('FIXED_AMOUNT') }}</option>
          </select>
        </div>
        <div>
          <label class="lbl">{{ t('saleOrderDetail.value') }}</label>
          <input v-model.number="discountValue" type="number" min="0.01" step="0.01" class="field w-32" />
        </div>
        <button type="submit" class="btn" :disabled="saving">{{ t('common.apply') }}</button>
      </form>
      <table class="min-w-full">
        <thead><tr class="border-b border-gray-100 dark:border-gray-800"><th class="th">{{ t('common.type') }}</th><th class="th">{{ t('saleOrderDetail.value') }}</th><th class="th">{{ t('common.sum') }}</th><th class="th">{{ t('common.date') }}</th></tr></thead>
        <tbody>
          <tr v-if="discounts.length === 0"><td colspan="4" class="empty">{{ t('saleOrderDetail.noHistory') }}</td></tr>
          <tr v-for="d in discounts" :key="d.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ d.discountType ? discountTypeLabel(d.discountType) : '—' }}</td>
            <td class="td">{{ d.discountValue }}</td>
            <td class="td">{{ money(d.discountAmount) }}</td>
            <td class="td">{{ formatDate(d.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'history'" class="card">
      <table class="min-w-full">
        <thead><tr class="border-b border-gray-100 dark:border-gray-800"><th class="th">{{ t('common.from') }}</th><th class="th">{{ t('saleOrderDetail.historyTo') }}</th><th class="th">{{ t('saleOrderDetail.who') }}</th><th class="th">{{ t('common.date') }}</th></tr></thead>
        <tbody>
          <tr v-if="history.length === 0"><td colspan="4" class="empty">{{ t('saleOrderDetail.noHistory') }}</td></tr>
          <tr v-for="h in history" :key="h.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td"><SaleStatusBadge v-if="h.fromStatus" :status="h.fromStatus" /><span v-else>—</span></td>
            <td class="td"><SaleStatusBadge :status="h.toStatus" /></td>
            <td class="td">{{ h.changedByUserFullName || h.changedByUsername || '—' }}</td>
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

    <div v-if="orderModal" class="overlay">
      <div class="modal modal-lg" role="dialog" aria-modal="true" aria-labelledby="order-modal-title">
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span class="modal-badge"><Pencil class="h-5 w-5" /></span>
            <div>
              <h3 id="order-modal-title" class="title">{{ t('saleOrderDetail.editOrderTitle', { id: order?.id }) }}</h3>
              <p class="modal-hint mt-1">{{ t('saleOrderDetail.editOrderSubtitle') }}</p>
            </div>
          </div>
          <button type="button" class="close-btn" :aria-label="t('common.close')" @click="orderModal = false">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-4" @submit.prevent="onOrderSave">
          <div>
            <label class="f-lbl">{{ t('common.client') }}</label>
            <SearchableSelect
              v-model="orderForm.clientId"
              :options="clientOptions"
              :placeholder="t('saleOrderDetail.clientPlaceholder')"
              :search-placeholder="t('payments.clientSearchPlaceholder')"
            />
          </div>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label for="order-wh" class="f-lbl">{{ t('common.warehouse') }} <span class="req">*</span></label>
              <div class="relative">
                <WarehouseIcon class="f-icon" />
                <select
                  id="order-wh"
                  v-model.number="orderForm.warehouseId"
                  required
                  class="field f-field"
                  :disabled="order?.orderStatus === 'COMPLETED'"
                >
                  <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
                </select>
              </div>
            </div>
            <div>
              <label for="order-user" class="f-lbl">{{ t('saleOrderDetail.seller') }} <span class="req">*</span></label>
              <div class="relative">
                <UserCog class="f-icon" />
                <select id="order-user" v-model.number="orderForm.userId" required class="field f-field">
                  <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
                </select>
              </div>
            </div>
          </div>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label for="order-date" class="f-lbl">{{ t('common.date') }} <span class="req">*</span></label>
              <div class="relative">
                <CalendarDays class="f-icon" />
                <input id="order-date" v-model="orderForm.orderDate" type="datetime-local" required class="field f-field" />
              </div>
            </div>
            <div>
              <label for="order-sum" class="f-lbl">{{ t('saleOrderDetail.orderSum') }} <span class="req">*</span></label>
              <div class="relative">
                <Banknote class="f-icon" />
                <input
                  id="order-sum"
                  :value="orderSumText"
                  inputmode="decimal"
                  autocomplete="off"
                  required
                  placeholder="0"
                  class="field f-field"
                  @input="onOrderSumInput"
                />
              </div>
            </div>
          </div>
          <div>
            <label class="f-lbl">{{ t('saleOrderCreate.deliveryTitle') }}</label>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <button
                type="button"
                class="delivery-opt"
                :class="{ active: orderForm.deliveryType === 'PICKUP' }"
                :disabled="deliveryTypeLocked"
                @click="orderForm.deliveryType = 'PICKUP'"
              >
                <PackageCheck class="h-5 w-5 flex-shrink-0" />
                <span class="opt-title">{{ t('saleOrderCreate.pickup') }}</span>
              </button>
              <button
                type="button"
                class="delivery-opt"
                :class="{ active: orderForm.deliveryType === 'DELIVERY' }"
                :disabled="deliveryTypeLocked"
                @click="orderForm.deliveryType = 'DELIVERY'"
              >
                <Truck class="h-5 w-5 flex-shrink-0" />
                <span class="opt-title">{{ t('saleOrderCreate.delivery') }}</span>
              </button>
            </div>
            <div v-if="orderForm.deliveryType === 'DELIVERY'" class="relative mt-3">
              <Truck class="f-icon" />
              <input
                :value="deliveryFeeText"
                inputmode="decimal"
                autocomplete="off"
                placeholder="0"
                class="field f-field"
                :aria-label="t('saleOrderCreate.deliveryFee')"
                @input="onDeliveryFeeInput"
              />
              <span class="f-suffix">{{ t('saleOrderCreate.deliveryFee') }}</span>
            </div>
          </div>
          <div>
            <label for="order-comment" class="f-lbl">{{ t('common.comment') }}</label>
            <div class="relative">
              <MessageSquare class="f-icon f-icon-top" />
              <textarea
                id="order-comment"
                v-model="orderForm.comment"
                rows="2"
                class="field f-field f-textarea"
                :placeholder="t('saleOrderCreate.commentPlaceholder')"
              />
            </div>
          </div>
          <p v-if="order && order.warehouseId !== orderForm.warehouseId" class="modal-hint">
            {{ t('saleOrderDetail.warehouseChangeHint') }}
          </p>
          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="orderModal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn btn-with-icon" :disabled="saving">
              <Check class="h-4 w-4" />
              {{ saving ? t('common.saving') : t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="itemModal" class="overlay">
      <div class="modal">
        <h3 class="title mb-4">{{ itemEditingId ? t('saleOrderDetail.editItem') : t('saleOrderDetail.newItem') }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onItemSave">
          <select v-model.number="itemForm.goodsId" required class="field">
            <option :value="0" disabled>{{ t('common.product') }}</option>
            <option v-for="g in goods" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
          <div v-if="itemIsWindow" class="grid grid-cols-2 gap-3">
            <label class="modal-lbl">
              {{ t('saleOrderDetail.width') }}
              <input v-model.number="itemForm.width" type="number" min="1" step="1" required class="field" />
            </label>
            <label class="modal-lbl">
              {{ t('saleOrderDetail.height') }}
              <input v-model.number="itemForm.height" type="number" min="1" step="1" required class="field" />
            </label>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <label class="modal-lbl">
              {{ itemIsWindow ? t('saleOrderDetail.countPcs') : t('common.count') }}
              <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" required class="field" />
            </label>
            <label class="modal-lbl">
              {{ itemIsWindow ? t('saleOrderDetail.sellingKvm') : t('saleOrderDetail.selling') }}
              <input v-model.number="itemForm.priceSelling" type="number" min="0.01" step="0.01" required class="field" />
            </label>
          </div>
          <p v-if="itemIsWindow" class="modal-hint">
            {{ t('saleOrderDetail.kvmHint', { kvm: formatQty(itemKvm), sum: money(itemKvm * Number(itemForm.priceSelling || 0)) }) }}
          </p>
          <label class="modal-lbl">
            {{ t('saleOrderDetail.costPrice') }}
            <input v-model.number="itemForm.priceCost" type="number" min="0" step="0.01" required class="field" />
          </label>
          <p v-if="itemEditingId" class="modal-hint">
            {{ t('saleOrderDetail.editItemHint') }}
          </p>
          <input v-model="itemForm.arrivalDate" type="datetime-local" required class="field" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="itemModal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="saving">{{ t('common.save') }}</button>
          </div>
        </form>
      </div>
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
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowDown,
  ArrowUp,
  Banknote,
  CalendarDays,
  Check,
  Factory,
  ImagePlus,
  MessageSquare,
  PackageCheck,
  Pencil,
  Plus,
  Trash2,
  Truck,
  UserCog,
  Warehouse as WarehouseIcon,
  X,
} from 'lucide-vue-next'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import { useAuthStore } from '@/stores/auth'
import { formatUzPhone } from '@/utils/phone'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import SaleStatusBadge from '@/components/crm/SaleStatusBadge.vue'
import DeliveryStatusBadge from '@/components/crm/DeliveryStatusBadge.vue'
import AuthImage from '@/components/crm/AuthImage.vue'
import { nextSaleStatuses, type SaleStatus } from '@/utils/saleStatus'
import { fetchProductionBySaleOrder, sendToProduction, type ProductionOrder } from '@/api/production'
import { fetchDeliveryBySaleOrder, type Delivery } from '@/api/transport'
import { fetchActiveWorkshops, type Workshop } from '@/api/workshops'
import {
  applyDiscount,
  changeSaleOrderStatus,
  fetchDiscountHistory,
  fetchSaleOrder,
  fetchSaleOrderHistory,
  fetchSaleOrderImages,
  updateSaleOrder,
  uploadSaleOrderImages,
  deleteSaleOrderImage,
  type DeliveryType,
  type SaleOrder,
  type SaleOrderDiscountHistory,
  type SaleOrderHistory,
  type SaleOrderImage,
} from '@/api/sales'
import {
  createSaleOrderItem,
  deleteSaleOrderItem,
  fetchSaleOrderItems,
  updateSaleOrderItem,
  type SaleOrderItem,
} from '@/api/saleOrderItems'
import {
  createSaleOrderWaste,
  deleteSaleOrderWaste,
  fetchSaleOrderWastes,
  type SaleOrderWaste,
} from '@/api/saleOrderWastes'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchClients, type Client } from '@/api/clients'
import { fetchUserOptions, type UserItem } from '@/api/users'
import { fetchGoods, type Goods } from '@/api/goods'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { amountToText, formatAmountInput, formatDate, money, nowLocal, toApiDate } from '@/utils/format'

const { writeBlocked } = useFilialScope()
const { t, te } = useI18n()
const auth = useAuthStore()

const route = useRoute()
const TAB_IDS = ['items', 'discount', 'history', 'images', 'waste'] as const
const tabs = computed(() => TAB_IDS.map((id) => ({ id, label: t(`saleOrderDetail.tabs.${id}`) })))
const tab = ref<(typeof TAB_IDS)[number]>('items')

const order = ref<SaleOrder | null>(null)
const items = ref<SaleOrderItem[]>([])
const discounts = ref<SaleOrderDiscountHistory[]>([])
const history = ref<SaleOrderHistory[]>([])
const images = ref<SaleOrderImage[]>([])
const wastes = ref<SaleOrderWaste[]>([])
const warehouses = ref<Warehouse[]>([])
const clients = ref<Client[]>([])
const users = ref<UserItem[]>([])
const goods = ref<Goods[]>([])
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const saving = ref(false)
const discountType = ref('PERCENTAGE')
const discountValue = ref(10)

const orderModal = ref(false)
const orderForm = reactive({
  warehouseId: 0,
  clientId: 0,
  userId: 0,
  orderDate: '',
  totalSum: 0,
  comment: '',
  deliveryType: 'DELIVERY' as DeliveryType,
  deliveryFee: 0,
})
const orderSumText = ref('')
const deliveryFeeText = ref('')
const deliveryTypeLocked = computed(() =>
  ['IN_DELIVERY', 'DELIVERED', 'COMPLETED'].includes(order.value?.orderStatus || ''),
)

function onDeliveryFeeInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  deliveryFeeText.value = text
  el.value = text
  orderForm.deliveryFee = value
}
const clientOptions = computed(() => [
  { value: 0, label: t('saleOrderDetail.clientPlaceholder'), searchText: '' },
  ...clients.value.map((c) => {
    const phone = c.phone ? formatUzPhone(c.phone) : ''
    return {
      value: c.id,
      label: phone ? `${c.fullName} · ${phone}` : c.fullName,
      searchText: `${c.fullName} ${c.phone || ''}`,
    }
  }),
])

function onOrderSumInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  orderSumText.value = text
  el.value = text
  orderForm.totalSum = value
}
const itemModal = ref(false)
const itemEditingId = ref<number | null>(null)
const itemForm = reactive({ goodsId: 0, count: 1, width: 0, height: 0, priceCost: 0, priceSelling: 0, arrivalDate: '' })

function isWindowGoodsId(goodsId?: number | null) {
  const g = goods.value.find((x) => x.id === goodsId)
  return (g?.type || '').toUpperCase() === 'WINDOW'
}

const itemIsWindow = computed(() => isWindowGoodsId(itemForm.goodsId))
const itemKvm = computed(() =>
  itemIsWindow.value
    ? (Number(itemForm.width || 0) * Number(itemForm.height || 0) * Number(itemForm.count || 0)) / 10000
    : 0,
)

function formatQty(v?: number | null) {
  if (v == null || Number.isNaN(Number(v))) return '—'
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v))
}

function windowPieces(it: SaleOrderItem) {
  const w = Number(it.width || 0)
  const h = Number(it.height || 0)
  if (!(w > 0 && h > 0)) return null
  return (Number(it.count || 0) * 10000) / (w * h)
}

function itemQtyText(it: SaleOrderItem) {
  if (!isWindowGoodsId(it.goodsId)) return formatQty(it.count)
  const pieces = windowPieces(it)
  const kvm = `${formatQty(it.count)} ${t('saleOrderDetail.kvm')}`
  return pieces == null ? kvm : `${kvm} (${formatQty(it.width)}×${formatQty(it.height)}, ${formatQty(pieces)} ${t('saleOrderDetail.pcs')})`
}

watch(
  () => itemForm.goodsId,
  (id) => {
    if (!itemModal.value || !isWindowGoodsId(id)) return
    const g = goods.value.find((x) => x.id === id)
    if (!itemForm.width) itemForm.width = Number(g?.width || 0)
    if (!itemForm.height) itemForm.height = Number(g?.height || 0)
  },
)
const wasteModal = ref(false)
const wasteForm = reactive({ goodsId: 0, quantity: 1, comment: '' })
const statusSaving = ref(false)
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

const MAIN_FLOW: SaleStatus[] = ['NEW', 'CONFIRMED', 'PROCESSING', 'READY', 'IN_DELIVERY', 'DELIVERED', 'COMPLETED']
const flowStatuses = computed<SaleStatus[]>(() => {
  const status = order.value?.orderStatus
  const selfPickup = !delivery.value && (status === 'DELIVERED' || status === 'COMPLETED')
  const base = selfPickup ? MAIN_FLOW.filter((s) => s !== 'IN_DELIVERY') : MAIN_FLOW
  return status === 'CANCELLED' ? [...base, 'CANCELLED'] : base
})
const deliveryConfirmBlocked = computed(
  () =>
    order.value?.orderStatus === 'IN_DELIVERY' &&
    !(delivery.value && ['IN_TRANSIT', 'ARRIVED'].includes(delivery.value.deliveryStatus)),
)
const flowIndex = computed(() => flowStatuses.value.indexOf(order.value?.orderStatus as SaleStatus))
const doneIndex = computed(() => (order.value?.orderStatus === 'CANCELLED' ? -1 : flowIndex.value))
const nextStatuses = computed(() =>
  nextSaleStatuses(order.value?.orderStatus).filter(
    (st) => !(st === 'IN_DELIVERY' && order.value?.deliveryType === 'PICKUP'),
  ),
)

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
    const [o, its, disc, hist, imgs, w, wh, cl, us, gs] = await Promise.all([
      fetchSaleOrder(id()),
      fetchSaleOrderItems(id()),
      fetchDiscountHistory(id()),
      fetchSaleOrderHistory(id()),
      fetchSaleOrderImages(id()),
      fetchSaleOrderWastes(id()),
      fetchWarehouses(),
      fetchClients(),
      fetchUserOptions(),
      fetchGoods(),
    ])
    order.value = o.data
    items.value = its.data || []
    discounts.value = disc.data || []
    history.value = hist.data || []
    images.value = imgs.data || []
    wastes.value = w.data || []
    warehouses.value = wh.data || []
    clients.value = cl.data || []
    users.value = us.data || []
    goods.value = gs.data || []
    await Promise.all([loadDelivery(), loadProduction()])
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  }
}

function openEdit() {
  if (!order.value) return
  orderForm.warehouseId = order.value.warehouseId || 0
  orderForm.clientId = order.value.clientId || 0
  orderForm.userId = order.value.userId || 0
  orderForm.orderDate = (order.value.orderDate || '').slice(0, 16)
  orderForm.totalSum = Number(order.value.originalTotalSum ?? order.value.totalSum ?? 0)
  orderSumText.value = amountToText(orderForm.totalSum)
  orderForm.comment = order.value.comment || ''
  orderForm.deliveryType = order.value.deliveryType || 'DELIVERY'
  orderForm.deliveryFee = Number(order.value.deliveryFee || 0)
  deliveryFeeText.value = orderForm.deliveryFee ? amountToText(orderForm.deliveryFee) : ''
  formError.value = null
  orderModal.value = true
}

async function onOrderSave() {
  saving.value = true
  formError.value = null
  try {
    await updateSaleOrder(id(), {
      warehouseId: orderForm.warehouseId,
      userId: orderForm.userId,
      orderDate: toApiDate(orderForm.orderDate),
      totalSum: orderForm.totalSum,
      clientId: orderForm.clientId || null,
      comment: orderForm.comment.trim(),
      deliveryType: orderForm.deliveryType,
      deliveryFee: orderForm.deliveryType === 'DELIVERY' ? orderForm.deliveryFee : 0,
    })
    orderModal.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

function fillItemFromGoods() {
  const g = goods.value.find((x) => x.id === itemForm.goodsId)
  if (g) {
    itemForm.priceCost = Number(g.priceCost || 0)
    itemForm.priceSelling = Number(g.priceSelling || 0)
  }
}

function openItemCreate() {
  itemEditingId.value = null
  itemForm.goodsId = goods.value[0]?.id || 0
  itemForm.count = 1
  const g = goods.value.find((x) => x.id === itemForm.goodsId)
  itemForm.width = isWindowGoodsId(itemForm.goodsId) ? Number(g?.width || 0) : 0
  itemForm.height = isWindowGoodsId(itemForm.goodsId) ? Number(g?.height || 0) : 0
  itemForm.arrivalDate = nowLocal()
  fillItemFromGoods()
  formError.value = null
  itemModal.value = true
}

function openItemEdit(it: SaleOrderItem) {
  itemEditingId.value = it.id
  itemForm.goodsId = it.goodsId || 0
  const pieces = isWindowGoodsId(it.goodsId) ? windowPieces(it) : null
  itemForm.count = pieces != null ? Math.round(pieces * 10000) / 10000 : Number(it.count || 1)
  itemForm.width = Number(it.width || 0)
  itemForm.height = Number(it.height || 0)
  itemForm.priceCost = Number(it.priceCost || 0)
  itemForm.priceSelling = Number(it.priceSelling || 0)
  itemForm.arrivalDate = (it.arrivalDate || '').slice(0, 16)
  formError.value = null
  itemModal.value = true
}

async function onItemSave() {
  if (!order.value) return
  saving.value = true
  formError.value = null
  try {
    const payload = {
      warehouseId: order.value.warehouseId || 0,
      saleOrderId: order.value.id,
      clientId: order.value.clientId || 0,
      goodsId: itemForm.goodsId,
      priceCost: itemForm.priceCost,
      priceSelling: itemForm.priceSelling,
      count: itemForm.count,
      width: itemIsWindow.value ? itemForm.width : undefined,
      height: itemIsWindow.value ? itemForm.height : undefined,
      arrivalDate: toApiDate(itemForm.arrivalDate),
    }
    if (itemEditingId.value) await updateSaleOrderItem(itemEditingId.value, payload)
    else await createSaleOrderItem(payload)
    itemModal.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e, t('saleOrderDetail.itemSaveError'))
  } finally {
    saving.value = false
  }
}

async function onItemDelete(it: SaleOrderItem) {
  if (!confirm(t('saleOrderDetail.deleteItemConfirm'))) return
  try {
    await deleteSaleOrderItem(it.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onDiscount() {
  saving.value = true
  try {
    await applyDiscount(id(), discountType.value, discountValue.value)
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
  if (status === 'CANCELLED' && !confirm(t('saleOrderDetail.cancelConfirm', { id: order.value.id }))) return
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

onMounted(load)

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
.head { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.lbl { display: block; margin-bottom: 0.25rem; font-size: 0.75rem; color: #6b7280; }
.tab { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
.modal-lbl { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.modal-hint { font-size: 0.75rem; color: #6b7280; }
.dark .modal-lbl, .dark .modal-hint { color: #9ca3af; }
.sub { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.status-head { display: flex; justify-content: space-between; gap: 1rem; }
.status-flow { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; margin-top: 1rem; }
.flow-step { border-radius: 9999px; border: 1px solid #e5e7eb; padding: 0.25rem 0.75rem; font-size: 0.75rem; color: #6b7280; background: #f9fafb; }
.flow-step.done { border-color: #c7d2fe; background: #eef2ff; color: #4338ca; }
.flow-step.current { border-color: #465fff; background: #465fff; color: #fff; font-weight: 600; }
.flow-step.current.cancelled { border-color: #dc2626; background: #dc2626; }
.flow-sep { height: 2px; width: 1.25rem; background: #e5e7eb; }
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
.dark .flow-step { border-color: #374151; background: #1f2937; color: #9ca3af; }
.dark .flow-step.done { border-color: rgb(70 95 255 / 45%); background: rgb(70 95 255 / 15%); color: #a5b4fc; }
.dark .flow-step.current { border-color: #465fff; background: #465fff; color: #fff; }
.dark .flow-step.current.cancelled { border-color: #dc2626; background: #dc2626; }
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
.modal-lg { max-width: 36rem; max-height: calc(100vh - 2rem); overflow-y: auto; }
.modal-badge { display: flex; height: 2.5rem; width: 2.5rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.75rem; background: #eff4ff; color: #465fff; }
.close-btn { display: flex; height: 2.25rem; width: 2.25rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; color: #9ca3af; }
.close-btn:hover { background: #f3f4f6; color: #374151; }
.f-lbl { display: block; margin-bottom: 0.375rem; font-size: 0.875rem; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.f-icon { position: absolute; top: 50%; left: 0.75rem; z-index: 1; height: 1.1rem; width: 1.1rem; transform: translateY(-50%); color: #98a2b3; pointer-events: none; }
.f-field { height: 2.75rem; padding-left: 2.5rem; }
.f-field:focus { outline: none; border-color: #9cb0ff; box-shadow: 0 0 0 4px rgb(70 95 255 / 10%); }
.f-field:disabled { opacity: 0.6; cursor: not-allowed; }
.dark .modal-badge { background: rgb(70 95 255 / 12%); color: #9cb0ff; }
.dark .close-btn:hover { background: rgb(255 255 255 / 5%); color: rgba(255, 255, 255, 0.8); }
.dark .f-lbl { color: #9ca3af; }
.dark .f-field { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.dark .f-field option { background: #101828; }
.f-icon-top { top: 0.8rem; transform: none; }
.f-textarea { height: auto; min-height: 4.5rem; padding-top: 0.6rem; padding-bottom: 0.6rem; resize: vertical; line-height: 1.4; }
.f-suffix { position: absolute; top: 50%; right: 0.75rem; transform: translateY(-50%); font-size: 0.75rem; color: #98a2b3; pointer-events: none; }
.delivery-opt { display: flex; align-items: center; gap: 0.6rem; height: 2.75rem; border-radius: 0.6rem; border: 1px solid #d1d5db; padding: 0 0.85rem; color: #6b7280; transition: border-color .15s, background-color .15s; }
.delivery-opt:hover:not(:disabled) { border-color: #9cb0ff; }
.delivery-opt:disabled { opacity: 0.55; cursor: not-allowed; }
.delivery-opt.active { border-color: #465fff; background: #eff4ff; color: #465fff; }
.opt-title { font-size: 0.875rem; font-weight: 600; color: #1f2937; }
.delivery-opt.active .opt-title { color: #465fff; }
.dark .delivery-opt { border-color: #344054; color: #9ca3af; }
.dark .delivery-opt.active { border-color: #7592ff; background: rgb(70 95 255 / 12%); color: #9cb0ff; }
.dark .opt-title { color: rgba(255, 255, 255, 0.9); }
.dark .delivery-opt.active .opt-title { color: #9cb0ff; }
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
