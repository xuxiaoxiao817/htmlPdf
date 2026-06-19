import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export const listLanguages = () => http.get('/languages').then(r => r.data)

export const listAgreements = () => http.get('/agreements').then(r => r.data)

export const createAgreement = (data) => http.post('/agreements', data).then(r => r.data)

export const deleteAgreement = (id) => http.delete(`/agreements/${id}`).then(r => r.data)

export async function downloadAgreementPdf(id) {
  const resp = await http.get(`/agreements/${id}/pdf`, { responseType: 'blob' })
  // try to read filename from Content-Disposition
  const cd = resp.headers['content-disposition'] || ''
  const m = cd.match(/filename="?([^";]+)"?/i)
  const filename = m ? m[1] : `agreement_${id}.pdf`
  const url = URL.createObjectURL(resp.data)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
