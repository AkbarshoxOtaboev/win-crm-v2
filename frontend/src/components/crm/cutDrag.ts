import { reactive } from 'vue'

export type CutSource = 'sheet' | 'pick'

export interface CutDragPiece {
  itemId: number
  source: CutSource
  index: number
}

/** Kesish chizmasidagi bo'lakni boshqa chizmaga olib o'tish. */
export const cutDrag = reactive({
  piece: null as CutDragPiece | null,
})
