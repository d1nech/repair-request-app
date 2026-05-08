import { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  createRequest,
  deleteRequest,
  fetchRequests,
  getErrorMessage,
  login,
  register,
  updateRequest,
  updateStatus
} from './api';
import { AuthResponse, Priority, RepairRequest, RequestForm, Status } from './types';

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
  const [form, setForm] = useState<RequestForm>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const isAdmin = user?.role === 'ADMIN';

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

  async function handleDelete(id: number) {
    if (!confirm('Удалить заявку?')) return;
    try {
      await deleteRequest(id);
      await loadRequests();
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
            <b>Тест:</b> user@example.com / user12345<br />
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

      <section className="panel">
        <h2>{isAdmin ? 'Все заявки' : 'Мои заявки'}</h2>
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
                  {isAdmin && <div><dt>Автор</dt><dd>{request.userEmail}</dd></div>}
                </dl>
                <div className="actions">
                  <button onClick={() => startEdit(request)}>Редактировать</button>
                  {isAdmin && (
                    <select value={request.status} onChange={(e) => handleStatus(request.id, e.target.value as Status)}>
                      {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                    </select>
                  )}
                  <button className="danger" onClick={() => handleDelete(request.id)}>Удалить</button>
                </div>
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
