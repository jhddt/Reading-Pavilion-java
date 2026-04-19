import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'

type ImagePreview = {
  id: string
  file: File
  url: string
  width: number
  height: number
}

type CreateMode = 'document' | 'image' | 'text'
type DropArea = 'image' | 'document' | null

export function EssayCreatePage() {
  const { token } = useAuth()
  const navigate = useNavigate()
  const imageInputRef = useRef<HTMLInputElement | null>(null)
  const editorImageRef = useRef<HTMLImageElement | null>(null)
  const [mode, setMode] = useState<CreateMode>('document')
  const [dropArea, setDropArea] = useState<DropArea>(null)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [imageTitle, setImageTitle] = useState('')
  const [docTitle, setDocTitle] = useState('')
  const [images, setImages] = useState<ImagePreview[]>([])
  const [docFile, setDocFile] = useState<File | null>(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loadingText, setLoadingText] = useState(false)
  const [loadingImage, setLoadingImage] = useState(false)
  const [loadingDoc, setLoadingDoc] = useState(false)
  const [dragIndex, setDragIndex] = useState<number | null>(null)
  const [editorOpen, setEditorOpen] = useState(false)
  const [editingIndex, setEditingIndex] = useState<number | null>(null)
  const [cropRotate, setCropRotate] = useState(0)
  const [imageZoom, setImageZoom] = useState(1)
  const [cropBox, setCropBox] = useState({
    x: 6,
    y: 6,
    width: 88,
    height: 88,
  })
  const [dragMode, setDragMode] = useState<
    null | 'move' | 'n' | 's' | 'w' | 'e' | 'nw' | 'ne' | 'sw' | 'se'
  >(null)
  const dragStartRef = useRef<{
    x: number
    y: number
    box: { x: number; y: number; width: number; height: number }
  } | null>(null)

  const editingImage = editingIndex != null ? images[editingIndex] : null
  const imageInputId = 'essay-image-input'
  const documentInputId = 'essay-document-input'
  const imageFooterText = images.length
    ? images.length === 1
      ? images[0].file.name
      : `${images[0].file.name} 等 ${images.length} 个文件`
    : 'Not selected file'

  const handleSuccess = (essayId: number) => {
    setMessage(`创建成功，作文编号 ${essayId}`)
    setTimeout(() => navigate(`/essays/${essayId}`), 600)
  }

  const readImageMeta = (file: File) =>
    new Promise<ImagePreview>((resolve) => {
      const url = URL.createObjectURL(file)
      const img = new Image()
      img.onload = () => {
        resolve({
          id: `${file.name}-${file.lastModified}-${Math.random().toString(36).slice(2, 8)}`,
          file,
          url,
          width: img.width,
          height: img.height,
        })
      }
      img.src = url
    })

  const appendImages = async (files: File[]) => {
    if (!files.length) return
    const next = await Promise.all(files.map(readImageMeta))
    setImages((prev) => [...prev, ...next])
  }

  const onImageSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files || [])
    if (!files.length) return
    await appendImages(files)
    if (imageInputRef.current) imageInputRef.current.value = ''
  }

  useEffect(() => {
    const handlePaste = async (event: ClipboardEvent) => {
      if (mode !== 'image') return
      const items = Array.from(event.clipboardData?.items || [])
      const pastedImages = items
        .filter((item) => item.kind === 'file' && item.type.startsWith('image/'))
        .map((item) => item.getAsFile())
        .filter((file): file is File => Boolean(file))

      if (!pastedImages.length) return

      event.preventDefault()
      await appendImages(pastedImages)
    }

    window.addEventListener('paste', handlePaste)
    return () => {
      window.removeEventListener('paste', handlePaste)
    }
  }, [mode])

  const onDropImage = (dropIndex: number) => {
    if (dragIndex == null || dragIndex === dropIndex) return
    setImages((prev) => {
      const next = [...prev]
      const [item] = next.splice(dragIndex, 1)
      next.splice(dropIndex, 0, item)
      return next
    })
    setDragIndex(null)
  }

  const removeImage = (index: number) => {
    setImages((prev) => {
      const next = [...prev]
      URL.revokeObjectURL(next[index].url)
      next.splice(index, 1)
      return next
    })
  }

  const clearImages = () => {
    setImages((prev) => {
      prev.forEach((item) => URL.revokeObjectURL(item.url))
      return []
    })
    if (imageInputRef.current) {
      imageInputRef.current.value = ''
    }
  }

  const clearDocument = () => {
    setDocFile(null)
  }

  const handleDragOver = (event: React.DragEvent, area: Exclude<DropArea, null>) => {
    event.preventDefault()
    event.dataTransfer.dropEffect = 'copy'
    setDropArea(area)
  }

  const handleDragLeave = (event: React.DragEvent) => {
    if (event.currentTarget.contains(event.relatedTarget as Node)) return
    setDropArea(null)
  }

  const handleImageDrop = async (event: React.DragEvent) => {
    event.preventDefault()
    setDropArea(null)
    const files = Array.from(event.dataTransfer.files || []).filter((file) =>
      file.type.startsWith('image/'),
    )
    await appendImages(files)
  }

  const handleDocumentDrop = (event: React.DragEvent) => {
    event.preventDefault()
    setDropArea(null)
    const file = Array.from(event.dataTransfer.files || [])[0]
    if (file) setDocFile(file)
  }

  const openEditor = (index: number) => {
    setEditingIndex(index)
    setCropRotate(0)
    setImageZoom(1)
    setCropBox({
      x: 6,
      y: 6,
      width: 88,
      height: 88,
    })
    setEditorOpen(true)
  }

  const closeEditor = () => {
    setEditorOpen(false)
    setEditingIndex(null)
    setCropRotate(0)
    setImageZoom(1)
    setCropBox({
      x: 6,
      y: 6,
      width: 88,
      height: 88,
    })
    setDragMode(null)
    dragStartRef.current = null
  }

  useEffect(() => {
    if (!dragMode) return

    const handleMove = (event: MouseEvent) => {
      const image = editorImageRef.current
      const start = dragStartRef.current
      if (!image || !start) return

      const rect = image.getBoundingClientRect()
      const dx = ((event.clientX - start.x) / rect.width) * 100
      const dy = ((event.clientY - start.y) / rect.height) * 100
      const minSize = 8

      let next = { ...start.box }

      if (dragMode === 'move') {
        next.x = Math.max(0, Math.min(start.box.x + dx, 100 - start.box.width))
        next.y = Math.max(0, Math.min(start.box.y + dy, 100 - start.box.height))
      }

      if (dragMode.includes('e')) {
        next.width = Math.max(minSize, Math.min(start.box.width + dx, 100 - start.box.x))
      }
      if (dragMode.includes('s')) {
        next.height = Math.max(minSize, Math.min(start.box.height + dy, 100 - start.box.y))
      }
      if (dragMode.includes('w')) {
        const newX = Math.max(0, Math.min(start.box.x + dx, start.box.x + start.box.width - minSize))
        next.width = start.box.width - (newX - start.box.x)
        next.x = newX
      }
      if (dragMode.includes('n')) {
        const newY = Math.max(0, Math.min(start.box.y + dy, start.box.y + start.box.height - minSize))
        next.height = start.box.height - (newY - start.box.y)
        next.y = newY
      }

      setCropBox(next)
    }

    const handleUp = () => {
      setDragMode(null)
      dragStartRef.current = null
    }

    window.addEventListener('mousemove', handleMove)
    window.addEventListener('mouseup', handleUp)
    return () => {
      window.removeEventListener('mousemove', handleMove)
      window.removeEventListener('mouseup', handleUp)
    }
  }, [dragMode])

  const applyCrop = async () => {
    if (!editingImage || editingIndex == null) return
    const image = new Image()
    image.src = editingImage.url

    await new Promise<void>((resolve) => {
      image.onload = () => resolve()
    })

    const sourceWidth = image.width
    const sourceHeight = image.height
    const startX = (sourceWidth * cropBox.x) / 100
    const startY = (sourceHeight * cropBox.y) / 100
    const cropWidth = Math.max(40, (sourceWidth * cropBox.width) / 100)
    const cropHeight = Math.max(40, (sourceHeight * cropBox.height) / 100)
    const vertical = Math.abs(cropRotate) % 180 === 90

    const canvas = document.createElement('canvas')
    canvas.width = vertical ? cropHeight : cropWidth
    canvas.height = vertical ? cropWidth : cropHeight
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    ctx.translate(canvas.width / 2, canvas.height / 2)
    ctx.rotate((cropRotate * Math.PI) / 180)
    ctx.drawImage(
      image,
      startX,
      startY,
      cropWidth,
      cropHeight,
      -cropWidth / 2,
      -cropHeight / 2,
      cropWidth,
      cropHeight,
    )

    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.95))
    if (!blob) return

    const file = new File([blob], editingImage.file.name.replace(/\.[^.]+$/, '') + '-edited.jpg', {
      type: 'image/jpeg',
    })
    const nextPreview = await readImageMeta(file)

    setImages((prev) =>
      prev.map((item, index) => {
        if (index !== editingIndex) return item
        URL.revokeObjectURL(item.url)
        return nextPreview
      }),
    )
    closeEditor()
  }

  const orderedFiles = useMemo(() => images.map((item) => item.file), [images])
  const cropBoxStyle = useMemo(
    () => ({
      left: `${cropBox.x}%`,
      top: `${cropBox.y}%`,
      width: `${cropBox.width}%`,
      height: `${cropBox.height}%`,
    }),
    [cropBox],
  )

  const imageTransformStyle = useMemo(
    () => ({
      transform: `scale(${imageZoom}) rotate(${cropRotate}deg)`,
      transformOrigin: 'center center',
    }),
    [cropRotate, imageZoom],
  )

  const submitText = async (event: React.FormEvent) => {
    event.preventDefault()
    setLoadingText(true)
    setError('')
    setMessage('')
    try {
      const essayId = await api.post<number>('/essay/text', { title, content }, token)
      handleSuccess(essayId)
    } catch (err) {
      setError((err as Error).message || '创建失败')
    } finally {
      setLoadingText(false)
    }
  }

  const submitImages = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!orderedFiles.length) return
    setLoadingImage(true)
    setError('')
    setMessage('')
    try {
      const formData = new FormData()
      formData.append('title', imageTitle)
      orderedFiles.forEach((file) => formData.append('file', file))
      const essayId = await api.post<number>('/essay/image', formData, token)
      handleSuccess(essayId)
    } catch (err) {
      setError((err as Error).message || '图片创建失败')
    } finally {
      setLoadingImage(false)
    }
  }

  const submitDocument = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!docFile) return
    setLoadingDoc(true)
    setError('')
    setMessage('')
    try {
      const formData = new FormData()
      formData.append('title', docTitle)
      formData.append('file', docFile)
      const essayId = await api.post<number>('/essay/document', formData, token)
      handleSuccess(essayId)
    } catch (err) {
      setError((err as Error).message || '文档创建失败')
    } finally {
      setLoadingDoc(false)
    }
  }

  return (
    <div className="page-grid">
      <section className="panel create-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">创建作文</p>
            <h3>选择导入方式</h3>
          </div>
        </div>

        <div className="create-mode-tabs" role="tablist" aria-label="作文创建方式">
          <button
            type="button"
            className={`create-mode-tab ${mode === 'document' ? 'create-mode-tab-active' : ''}`}
            onClick={() => setMode('document')}
          >
            上传文件
          </button>
          <button
            type="button"
            className={`create-mode-tab ${mode === 'image' ? 'create-mode-tab-active' : ''}`}
            onClick={() => setMode('image')}
          >
            上传图片
          </button>
          <button
            type="button"
            className={`create-mode-tab ${mode === 'text' ? 'create-mode-tab-active' : ''}`}
            onClick={() => setMode('text')}
          >
            上传文本
          </button>
        </div>

        {mode === 'document' ? (
          <form className="form-grid" onSubmit={submitDocument}>
            <div>
              <p className="eyebrow">文档导入</p>
              <h3 className="create-mode-title">上传作文文档</h3>
            </div>
            <label className="field">
              <span>标题</span>
              <input value={docTitle} onChange={(event) => setDocTitle(event.target.value)} required />
            </label>
            <div className="field">
              <span>文档文件</span>
              <div className="upload-card-box">
                <label
                  htmlFor={documentInputId}
                  className={`upload-card-header ${dropArea === 'document' ? 'upload-card-header-drag' : ''}`}
                  onDragOver={(event) => handleDragOver(event, 'document')}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDocumentDrop}
                >
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M7 10V9C7 6.23858 9.23858 4 12 4C14.7614 4 17 6.23858 17 9V10C19.2091 10 21 11.7909 21 14C21 15.4806 20.1956 16.8084 19 17.5M7 10C4.79086 10 3 11.7909 3 14C3 15.4806 3.8044 16.8084 5 17.5M7 10C7.43285 10 7.84965 10.0688 8.24006 10.1959M12 12V21M12 12L15 15M12 12L9 15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  <p>Browse File to upload!</p>
                </label>
                <label htmlFor={documentInputId} className="upload-card-footer">
                  <svg viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
                    <path d="M15.331 6H8.5v20h15V14.154h-8.169z" />
                    <path d="M18.153 6h-.009v5.342H23.5v-.002z" />
                  </svg>
                  <p>{docFile?.name || 'Not selected file'}</p>
                  <button
                    type="button"
                    className="upload-card-trash"
                    onClick={(event) => {
                      event.preventDefault()
                      event.stopPropagation()
                      clearDocument()
                    }}
                    disabled={!docFile}
                  >
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M5.16565 10.1534C5.07629 8.99181 5.99473 8 7.15975 8H16.8402C18.0053 8 18.9237 8.9918 18.8344 10.1534L18.142 19.1534C18.0619 20.1954 17.193 21 16.1479 21H7.85206C6.80699 21 5.93811 20.1954 5.85795 19.1534L5.16565 10.1534Z" stroke="currentColor" strokeWidth="2" />
                      <path d="M19.5 5H4.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      <path d="M10 3C10 2.44772 10.4477 2 11 2H13C13.5523 2 14 2.44772 14 3V5H10V3Z" stroke="currentColor" strokeWidth="2" />
                    </svg>
                  </button>
                </label>
                <input
                  id={documentInputId}
                  className="upload-card-input"
                  type="file"
                  accept=".doc,.docx,.pdf,.txt"
                  onChange={(event) => setDocFile(event.target.files?.[0] || null)}
                />
              </div>
            </div>
            <div className="helper-text">{docFile ? `当前文件：${docFile.name}` : '支持 doc、docx、pdf、txt'}</div>
            <button type="submit" className="primary-button" disabled={loadingDoc || !docFile}>
              {loadingDoc ? '上传中...' : '通过文档创建'}
            </button>
          </form>
        ) : null}

        {mode === 'image' ? (
          <form className="form-grid" onSubmit={submitImages}>
            <div>
              <p className="eyebrow">图片 OCR</p>
              <h3 className="create-mode-title">上传作文图片</h3>
            </div>
            <label className="field">
              <span>标题</span>
              <input value={imageTitle} onChange={(event) => setImageTitle(event.target.value)} required />
            </label>
            <div className="field">
              <span>图片文件</span>
              <div className="upload-card-box upload-card-box-large">
                <label
                  htmlFor={imageInputId}
                  className={`upload-card-header ${dropArea === 'image' ? 'upload-card-header-drag' : ''}`}
                  onDragOver={(event) => handleDragOver(event, 'image')}
                  onDragLeave={handleDragLeave}
                  onDrop={handleImageDrop}
                >
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M7 10V9C7 6.23858 9.23858 4 12 4C14.7614 4 17 6.23858 17 9V10C19.2091 10 21 11.7909 21 14C21 15.4806 20.1956 16.8084 19 17.5M7 10C4.79086 10 3 11.7909 3 14C3 15.4806 3.8044 16.8084 5 17.5M7 10C7.43285 10 7.84965 10.0688 8.24006 10.1959M12 12V21M12 12L15 15M12 12L9 15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  <p>Browse File to upload!</p>
                </label>
                <label htmlFor={imageInputId} className="upload-card-footer">
                  <svg viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
                    <path d="M15.331 6H8.5v20h15V14.154h-8.169z" />
                    <path d="M18.153 6h-.009v5.342H23.5v-.002z" />
                  </svg>
                  <p>{imageFooterText}</p>
                  <button
                    type="button"
                    className="upload-card-trash"
                    onClick={(event) => {
                      event.preventDefault()
                      event.stopPropagation()
                      clearImages()
                    }}
                    disabled={!images.length}
                  >
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M5.16565 10.1534C5.07629 8.99181 5.99473 8 7.15975 8H16.8402C18.0053 8 18.9237 8.9918 18.8344 10.1534L18.142 19.1534C18.0619 20.1954 17.193 21 16.1479 21H7.85206C6.80699 21 5.93811 20.1954 5.85795 19.1534L5.16565 10.1534Z" stroke="currentColor" strokeWidth="2" />
                      <path d="M19.5 5H4.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      <path d="M10 3C10 2.44772 10.4477 2 11 2H13C13.5523 2 14 2.44772 14 3V5H10V3Z" stroke="currentColor" strokeWidth="2" />
                    </svg>
                  </button>
                </label>
                <input
                  id={imageInputId}
                  ref={imageInputRef}
                  className="upload-card-input"
                  type="file"
                  multiple
                  accept=".jpg,.jpeg,.png,.gif,.bmp"
                  onChange={onImageSelect}
                />
              </div>
            </div>
            <div className="helper-text">已选择 {images.length} 张图片，可拖拽排序，双击卡片进入裁切编辑，也支持直接 `Ctrl+V` 粘贴截图。</div>
            {images.length ? (
              <div className="image-preview-grid">
                {images.map((item, index) => (
                  <div
                    key={item.id}
                    className="image-preview-card"
                    draggable
                    onDragStart={() => setDragIndex(index)}
                    onDragOver={(event) => event.preventDefault()}
                    onDrop={() => onDropImage(index)}
                    onDoubleClick={() => openEditor(index)}
                  >
                    <div className="image-preview-order">{index + 1}</div>
                    <img src={item.url} alt={item.file.name} />
                    <div className="image-preview-meta">
                      <span>{item.width} × {item.height}</span>
                      <button type="button" className="text-button" onClick={() => openEditor(index)}>
                        编辑
                      </button>
                      <button type="button" className="text-button danger-text" onClick={() => removeImage(index)}>
                        删除
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : null}
            <button type="submit" className="primary-button" disabled={loadingImage || !images.length}>
              {loadingImage ? 'OCR 处理中...' : '通过图片创建'}
            </button>
          </form>
        ) : null}

        {mode === 'text' ? (
          <form className="form-grid" onSubmit={submitText}>
            <div>
              <p className="eyebrow">文本创建</p>
              <h3 className="create-mode-title">直接输入作文内容</h3>
            </div>
            <label className="field">
              <span>标题</span>
              <input value={title} onChange={(event) => setTitle(event.target.value)} required />
            </label>
            <label className="field">
              <span>作文内容</span>
              <textarea value={content} onChange={(event) => setContent(event.target.value)} rows={14} required />
            </label>
            <div className="helper-text">当前字数 {content.length}</div>
            <button type="submit" className="primary-button" disabled={loadingText}>
              {loadingText ? '提交中...' : '保存文本草稿'}
            </button>
          </form>
        ) : null}
      </section>

      {message ? <div className="feedback success">{message}</div> : null}
      {error ? <div className="feedback error">{error}</div> : null}

      {editorOpen && editingImage ? (
        <div className="overlay" onClick={closeEditor}>
          <div className="modal-card image-editor-modal" onClick={(event) => event.stopPropagation()}>
            <div className="section-heading">
              <div>
                <p className="eyebrow">图片编辑</p>
                <h3>{editingImage.file.name}</h3>
              </div>
              <button type="button" className="secondary-button" onClick={closeEditor}>
                关闭
              </button>
            </div>
            <div className="image-editor-stage">
              <div className="image-editor-frame">
                <div
                  className="image-editor-canvas"
                  onWheel={(event) => {
                    event.preventDefault()
                    setImageZoom((prev) => {
                      const next = prev + (event.deltaY < 0 ? 0.1 : -0.1)
                      return Math.max(0.5, Math.min(3, Number(next.toFixed(2))))
                    })
                  }}
                >
                  <img
                    ref={editorImageRef}
                    src={editingImage.url}
                    alt={editingImage.file.name}
                    style={imageTransformStyle}
                  />
                  <div className="crop-overlay" style={imageTransformStyle}>
                    <div className="crop-shadow crop-shadow-top" style={{ height: `${cropBox.y}%` }} />
                    <div className="crop-shadow crop-shadow-left" style={{ top: `${cropBox.y}%`, height: `${cropBox.height}%`, width: `${cropBox.x}%` }} />
                    <div className="crop-shadow crop-shadow-right" style={{ top: `${cropBox.y}%`, height: `${cropBox.height}%`, left: `${cropBox.x + cropBox.width}%`, right: 0 }} />
                    <div className="crop-shadow crop-shadow-bottom" style={{ top: `${cropBox.y + cropBox.height}%`, bottom: 0 }} />
                    <div
                      className="crop-box"
                      style={cropBoxStyle}
                      onMouseDown={(event) => {
                        event.preventDefault()
                        setDragMode('move')
                        dragStartRef.current = {
                          x: event.clientX,
                          y: event.clientY,
                          box: cropBox,
                        }
                      }}
                    >
                      {(['n', 's', 'w', 'e', 'nw', 'ne', 'sw', 'se'] as const).map((handle) => (
                        <button
                          key={handle}
                          type="button"
                          className={`crop-handle crop-handle-${handle}`}
                          onMouseDown={(event) => {
                            event.preventDefault()
                            event.stopPropagation()
                            setDragMode(handle)
                            dragStartRef.current = {
                              x: event.clientX,
                              y: event.clientY,
                              box: cropBox,
                            }
                          }}
                        />
                      ))}
                    </div>
                  </div>
                </div>
              </div>
              <div className="form-grid">
                <div className="helper-text">直接在图片上拖动裁切框、边或四个角即可调整裁切区域，滚轮可以缩放图片。</div>
                <label className="field">
                  <span>图片缩放</span>
                  <input
                    type="range"
                    min="0.5"
                    max="3"
                    step="0.1"
                    value={imageZoom}
                    onChange={(event) => setImageZoom(Number(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span>旋转角度</span>
                  <input
                    type="range"
                    min="-180"
                    max="180"
                    step="90"
                    value={cropRotate}
                    onChange={(event) => setCropRotate(Number(event.target.value))}
                  />
                </label>
                <div className="action-row">
                  <button type="button" className="secondary-button" onClick={() => setCropRotate((prev) => prev - 90)}>
                    逆时针 90°
                  </button>
                  <button type="button" className="secondary-button" onClick={() => setCropRotate((prev) => prev + 90)}>
                    顺时针 90°
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => {
                      setCropRotate(0)
                      setImageZoom(1)
                      setCropBox({
                        x: 6,
                        y: 6,
                        width: 88,
                        height: 88,
                      })
                    }}
                  >
                    重置
                  </button>
                  <button type="button" className="primary-button" onClick={applyCrop}>
                    应用裁切
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
