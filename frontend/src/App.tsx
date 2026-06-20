import { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  addComment,
  assignMaster,
  classifyRequest,
  createRequest,
  deleteRequest,
  downloadAttachment,
  fetchAttachments,
  fetchCategories,
  fetchComments,
  fetchMasters,
  fetchRequests,
  fetchStatusHistory,
  getErrorMessage,
  login,
  register,
  updateRequest,
  updateStatus,
  uploadAttachment
} from './api';
import {
  Attachment,
  AuthResponse,
  Category,
  Comment,
  MasterOption,
  Priority,
  RepairRequest,
  RequestForm,
  Status,
  StatusHistoryEntry
} from './types';

const emptyForm: RequestForm = {
  title: '',
  description: '',
  equipmentType: '',
  location: '',
  priority: 'MEDIUM'
};

const statusLabels: Record<Status, string> = {
  NEW: 'Новая',
  IN_PROGRESS: 'В работе',
  WAITING_PARTS: 'Ожидание деталей',
  DONE: 'Выполнена',
  CANCELLED: 'Отменена'
};

const priorityLabels: Record<Priority, string> = {
  LOW: 'Низкий',
  MEDIUM: 'Средний',
  HIGH: 'Высокий',
  CRITICAL: 'Критический'
};

function App() {
  const [user, setUser] = useState<AuthResponse | null>(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('user@example.com');
  const [password, setPassword] = useState('user12345');
  const [fullName, setFullName] = useState('Новый пользователь');
  const [requests, setRequests] = useState<RepairRequest[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [masters, setMasters] = useState<MasterOption[]>([]);
  const [form, setForm] = useState<RequestForm>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [detailsLoading, setDetailsLoading] = useState<number | null>(null);
  const [comments, setComments] = useState<Record<number, Comment[]>>({});
  const [attachments, setAttachments] = useState<Record<number, Attachment[]>>({});
  const [history, setHistory] = useState<Record<number, StatusHistoryEntry[]>>({});
  const [commentDraft, setCommentDraft] = useState<Record<number, string>>({});

  const isAdmin = user?.role === 'ADMIN';
  const isOperator = user?.role === 'OPERATOR';
  const isMaster = user?.role === 'MASTER';
  const canDispatch = isAdmin || isOperator;
  const canCreate = user?.role === 'USER' || isAdmin;

  const stats = useMemo(() => {
    return requests.reduce(
      (acc, item) => {
        acc.total += 1;
        acc[item.status] += 1;
        return acc;
      },
      { total: 0, NEW: 0, IN_PROGRESS: 0, WAITING_PARTS: 0, DONE: 0, CANCELLED: 0 } as Record<Status | 'total', number>
    );
  }, [requests]);

  async function loadRequests() {
    if (!user) return;
    try {
      setLoading(true);
      const data = await fetchRequests();
      setRequests(data);
    } catch (error) {
      setMessage(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadRequests();
    if (!user) return;
    fetchCategories().then(setCategories).catch((error) => setMessage(getErrorMessage(error)));
    if (canDispatch) {
      fetchMasters().then(setMasters).catch((error) => setMessage(getErrorMessage(error)));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  async function handleAuth(event: FormEvent) {
    event.preventDefault();
    try {
      setMessage('');
      const data = mode === 'login'
        ? await login(email, password)
        : await register(email, fullName, password);
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data));
      setUser(data);
    } catch (error) {
      setMessage(getErrorMessage(error));
    }
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    try {
      setMessage('');
      if (editingId) {
        await updateRequest(editingId, form);
        setMessage('Заявка обновлена');
      } else {
        await createRequest(form);
        setMessage('Заявка создана');
      }
      setForm(emptyForm);
      setEditingId(null);
      await loadRequests();
    } catch (error) {
      setMessage(getErrorMessage(error));
    }
  }

  function startEdit(request: RepairRequest) {
    setEditingId(request.id);
    setForm({
      title: request.title,
      description: request.description,
      equipmentType: request.equipmentType,
      location: request.location,
      priority: request.priority
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async function handleStatus(id: number, status: Status) {
    try {
      await updateStatus(id, status);
      await loadRequests();
    } catch (error) {
      setMessage(getErrorMessage(error));
    }
  }

  async function handleAssign(id: number, masterId: number) {
    try {
      await assignMaster(id, masterId);
      await loadRequests();
    } catch (error) {
      setMessage(getErrorMessage(error));
    }
  }

  async function handleClassify(id: number, categoryId: number) {
    try {
      await classifyRequest(id, categoryId);
      await loadRequests();
    } catch (error) {
      setMessage(getErrorMessage(error));
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Удалить заявку?')) return;
    try {
      await deleteRequest(id);
      await loadRequests();
    } catch (error) {
      setMessage(getErrorMessage(error));
    }
  }

  async function loadDetails(id: number) {
    setDetailsLoading(id);
    try {
      const [c, a, h] = await Promise.all([
        fetchComments(id),
        fetchAttachments(id),
        fetchStatusHistory(id)
      ]);
      setComments((prev) => ({ ...prev, [id]: c }));
      setAttachments((prev) => ({ ...prev, [id]: a }));
      setHistory((prev) => ({ ...prev, [id]: h }));
    } catch (error) {
      setMessage(getErrorMessage(error));
    } finally {
      setDetailsLoading(null);
    }
  }

  function toggleDetails(id: number) {
    if (expandedId === id) {
      setExpandedId(null);
      return;
    }
    setExpandedId(id);
    if (!comments[id]) {
      loadDetails(id);
    }
  }

  async function handleAddComment(id: number) {
    const text = (commentDraft[id] || '').trim();
    if (!text) return;
    try {
      await addComment(id, text);
      setCommentDraft((prev) => ({ ...prev, [id]: '' }));
      const updated = await fetchComments(id);
      setComments((prev) => ({ ...prev, [id]: updated }));
    } catch (error) {
      setMessage(getErrorMessage(error));
    }
  }

  async function handleUpload(id: number, fileList: FileList | null) {
    const file = fileList?.[0];
    if (!file) return;
    try {
      await uploadAttachment(id, file);
      const updated = await fetchAttachments(id);
      setAttachments((prev) => ({ ...prev, [id]: updated }));
    } catch (error) {
      setMessage(getErrorMessage(error));
    }
  }

  async function handleDownload(requestId: number, attachmentId: number, fileName: string) {
    try {
      const blob = await downloadAttachment(requestId, attachmentId);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      setMessage(getErrorMessage(error));
    }
  }

  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    setRequests([]);
  }

  function canEditDelete(request: RepairRequest) {
    return isAdmin || request.userId === user?.userId;
  }

  function canChangeStatus(request: RepairRequest) {
    return isAdmin || isOperator || (isMaster && request.assignedMasterId === user?.userId);
  }

  function listTitle() {
    if (isAdmin) return 'Все заявки';
    if (isOperator) return 'Все заявки (диспетчеризация)';
    if (isMaster) return 'Назначенные мне заявки';
    return 'Мои заявки';
  }

  if (!user) {
    return (
      <main className="auth-page">
        <section className="auth-card">
          <h1>Заявки на ремонт</h1>
          <p>Клиент-серверное приложение обработки заявок на ремонт</p>
          <form onSubmit={handleAuth} className="form">
            {mode === 'register' && (
              <label>
                ФИО
                <input value={fullName} onChange={(e) => setFullName(e.target.value)} />
              </label>
            )}
            <label>
              Email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            </label>
            <label>
              Пароль
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            </label>
            <button type="submit">{mode === 'login' ? 'Войти' : 'Зарегистрироваться'}</button>
          </form>
          <button className="link-button" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
            {mode === 'login' ? 'Создать аккаунт' : 'Уже есть аккаунт'}
          </button>
          {message && <div className="message error">{message}</div>}
          <div className="hint">
            <b>Клиент:</b> user@example.com / user12345<br />
            <b>Мастер:</b> master@example.com / master12345<br />
            <b>Оператор:</b> operator@example.com / operator12345<br />
            <b>Админ:</b> admin@example.com / admin12345
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="app">
      <header className="header">
        <div>
          <h1>Система обработки заявок на ремонт</h1>
          <p>{user.fullName} · {user.email} · роль {user.role}</p>
        </div>
        <button onClick={logout}>Выйти</button>
      </header>

      <section className="stats">
        <div><b>{stats.total}</b><span>Всего</span></div>
        <div><b>{stats.NEW}</b><span>Новые</span></div>
        <div><b>{stats.IN_PROGRESS}</b><span>В работе</span></div>
        <div><b>{stats.DONE}</b><span>Выполнены</span></div>
      </section>

      {canCreate && (
        <section className="panel">
          <h2>{editingId ? 'Редактирование заявки' : 'Новая заявка'}</h2>
          <form onSubmit={handleSave} className="grid-form">
            <label>
              Тема
              <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
            </label>
            <label>
              Тип оборудования
              <input value={form.equipmentType} onChange={(e) => setForm({ ...form, equipmentType: e.target.value })} />
            </label>
            <label>
              Место
              <input value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} />
            </label>
            <label>
              Приоритет
              <select value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value as Priority })}>
                {Object.entries(priorityLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </label>
            <label className="wide">
              Описание
              <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </label>
            <div className="wide actions">
              <button type="submit">{editingId ? 'Сохранить' : 'Создать'}</button>
              {editingId && <button type="button" onClick={() => { setEditingId(null); setForm(emptyForm); }}>Отмена</button>}
            </div>
          </form>
          {message && <div className="message">{message}</div>}
        </section>
      )}
      {!canCreate && message && <div className="message">{message}</div>}

      <section className="panel">
        <h2>{listTitle()}</h2>
        {loading ? <p>Загрузка...</p> : (
          <div className="cards">
            {requests.map((request) => (
              <article className="request-card" key={request.id}>
                <div className="card-head">
                  <h3>{request.title}</h3>
                  <span className={`badge ${request.status.toLowerCase()}`}>{statusLabels[request.status]}</span>
                </div>
                <p>{request.description}</p>
                <dl>
                  <div><dt>Оборудование</dt><dd>{request.equipmentType}</dd></div>
                  <div><dt>Место</dt><dd>{request.location}</dd></div>
                  <div><dt>Приоритет</dt><dd>{priorityLabels[request.priority]}</dd></div>
                  {(isAdmin || isOperator) && <div><dt>Автор</dt><dd>{request.userEmail}</dd></div>}
                  <div><dt>Категория</dt><dd>{request.categoryName || 'Не определена'}</dd></div>
                  <div><dt>Мастер</dt><dd>{request.assignedMasterEmail || 'Не назначен'}</dd></div>
                </dl>
                <div className="actions">
                  {canEditDelete(request) && <button onClick={() => startEdit(request)}>Редактировать</button>}
                  {canChangeStatus(request) && (
                    <select value={request.status} onChange={(e) => handleStatus(request.id, e.target.value as Status)}>
                      {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                    </select>
                  )}
                  {canDispatch && (
                    <select
                      value={request.categoryId ?? ''}
                      onChange={(e) => e.target.value && handleClassify(request.id, Number(e.target.value))}
                    >
                      <option value="">Классифицировать...</option>
                      {categories.map((category) => (
                        <option key={category.id} value={category.id}>{category.name}</option>
                      ))}
                    </select>
                  )}
                  {canDispatch && (
                    <select
                      value={request.assignedMasterId ?? ''}
                      onChange={(e) => e.target.value && handleAssign(request.id, Number(e.target.value))}
                    >
                      <option value="">Назначить мастера...</option>
                      {masters.map((master) => (
                        <option key={master.id} value={master.id}>{master.fullName}</option>
                      ))}
                    </select>
                  )}
                  {canEditDelete(request) && <button className="danger" onClick={() => handleDelete(request.id)}>Удалить</button>}
                  <button type="button" onClick={() => toggleDetails(request.id)}>
                    {expandedId === request.id ? 'Скрыть детали' : 'Комментарии и файлы'}
                  </button>
                </div>

                {expandedId === request.id && (
                  <div className="details">
                    {detailsLoading === request.id ? <p>Загрузка деталей...</p> : (
                      <>
                        <div className="details-block">
                          <h4>Комментарии</h4>
                          {(comments[request.id] || []).map((comment) => (
                            <div className="comment" key={comment.id}>
                              <b>{comment.authorEmail}</b>: {comment.message}
                            </div>
                          ))}
                          {!(comments[request.id] || []).length && <p>Комментариев пока нет.</p>}
                          <div className="comment-form">
                            <input
                              placeholder="Новый комментарий"
                              value={commentDraft[request.id] || ''}
                              onChange={(e) => setCommentDraft((prev) => ({ ...prev, [request.id]: e.target.value }))}
                            />
                            <button type="button" onClick={() => handleAddComment(request.id)}>Отправить</button>
                          </div>
                        </div>

                        <div className="details-block">
                          <h4>Вложения</h4>
                          {(attachments[request.id] || []).map((attachment) => (
                            <div className="attachment" key={attachment.id}>
                              <button type="button" onClick={() => handleDownload(request.id, attachment.id, attachment.fileName)}>
                                {attachment.fileName}
                              </button>
                            </div>
                          ))}
                          {!(attachments[request.id] || []).length && <p>Файлов пока нет.</p>}
                          <input type="file" onChange={(e) => handleUpload(request.id, e.target.files)} />
                        </div>

                        <div className="details-block">
                          <h4>История статусов</h4>
                          {(history[request.id] || []).map((entry) => (
                            <div className="history-entry" key={entry.id}>
                              {entry.oldStatus ? statusLabels[entry.oldStatus] : '—'} → {statusLabels[entry.newStatus]}
                              {entry.changedByEmail && <span> · {entry.changedByEmail}</span>}
                            </div>
                          ))}
                          {!(history[request.id] || []).length && <p>История пуста.</p>}
                        </div>
                      </>
                    )}
                  </div>
                )}
              </article>
            ))}
            {!requests.length && <p>Заявок пока нет.</p>}
          </div>
        )}
      </section>
    </main>
  );
}

export default App;
