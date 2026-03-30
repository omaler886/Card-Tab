import { startTransition, useDeferredValue, useEffect, useState } from 'react'
import './App.css'
import {
  calculateGoalProgress,
  defaultChallenges,
  defaultLogs,
  generateNutritionPlan,
  generateTrainingPlan,
  getStandardApplications,
  getWeeklyAnalytics,
} from './planner'
import type {
  ChallengeItem,
  GoalItem,
  GoalMetric,
  NutritionPlan,
  SocialPost,
  StandardApplication,
  TrainingPlan,
  UserProfile,
  WorkoutLog,
} from './types'

type TabKey = 'profile' | 'training' | 'nutrition' | 'logs' | 'analytics' | 'community'

interface AppState {
  profile: UserProfile
  goals: GoalItem[]
  trainingPlan: TrainingPlan
  nutritionPlan: NutritionPlan
  standards: StandardApplication[]
  logs: WorkoutLog[]
  posts: SocialPost[]
  challenges: ChallengeItem[]
}

const STORAGE_KEY = 'fitness-platform-state-v1'

const metricUnits: Record<GoalMetric, string> = {
  weight: 'kg',
  body_fat: '%',
  strength: 'kg',
  distance: 'km',
  consistency: 'sessions',
}

const defaultProfile: UserProfile = {
  name: 'Alex',
  age: 30,
  sex: 'male',
  heightCm: 178,
  weightKg: 80,
  bodyFatPct: 19,
  goalType: 'muscle_gain',
  level: 'intermediate',
  equipment: 'full_gym',
  daysPerWeek: 5,
  recovery: 'medium',
  sleepHours: 7.2,
  stepsPerDay: 8000,
  injuries: '',
}

const defaultGoals: GoalItem[] = [
  {
    id: 'goal-1',
    title: 'Body fat to 15%',
    metric: 'body_fat',
    targetValue: 15,
    currentValue: 19,
    unit: '%',
    deadline: '2026-08-01',
  },
  {
    id: 'goal-2',
    title: 'Bench press 100kg',
    metric: 'strength',
    targetValue: 100,
    currentValue: 85,
    unit: 'kg',
    deadline: '2026-09-15',
  },
]

const defaultPosts: SocialPost[] = [
  {
    id: 'post-a',
    author: 'Mina',
    content: 'Week 2 complete. Progressive overload worked great on squat.',
    tags: ['strength', 'progress'],
    likes: 34,
    comments: 8,
    createdAt: '2026-03-28T08:12:00.000Z',
  },
  {
    id: 'post-b',
    author: 'Jordan',
    content: 'Meal prep + hydration tracking improved recovery quality.',
    tags: ['nutrition', 'recovery'],
    likes: 19,
    comments: 4,
    createdAt: '2026-03-29T11:30:00.000Z',
  },
]

const tabs: { key: TabKey; label: string }[] = [
  { key: 'profile', label: 'Profile & Goals' },
  { key: 'training', label: 'Training Plan' },
  { key: 'nutrition', label: 'Nutrition Plan' },
  { key: 'logs', label: 'Workout Logs' },
  { key: 'analytics', label: 'Analytics' },
  { key: 'community', label: 'Community' },
]

function uid(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.round(Math.random() * 9999)}`
}

function buildSeedState(): AppState {
  const trainingPlan = generateTrainingPlan(defaultProfile)
  const nutritionPlan = generateNutritionPlan(defaultProfile)
  return {
    profile: defaultProfile,
    goals: defaultGoals,
    trainingPlan,
    nutritionPlan,
    standards: getStandardApplications(defaultProfile, trainingPlan, nutritionPlan),
    logs: defaultLogs(),
    posts: defaultPosts,
    challenges: defaultChallenges(),
  }
}

function loadState(): AppState {
  if (typeof window === 'undefined') return buildSeedState()
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) return buildSeedState()
  try {
    const parsed = JSON.parse(raw) as Partial<AppState>
    const base = buildSeedState()
    const profile = parsed.profile ?? base.profile
    const trainingPlan = parsed.trainingPlan ?? generateTrainingPlan(profile)
    const nutritionPlan = parsed.nutritionPlan ?? generateNutritionPlan(profile)
    return {
      profile,
      goals: parsed.goals ?? base.goals,
      trainingPlan,
      nutritionPlan,
      standards: parsed.standards ?? getStandardApplications(profile, trainingPlan, nutritionPlan),
      logs: parsed.logs ?? base.logs,
      posts: parsed.posts ?? base.posts,
      challenges: parsed.challenges ?? base.challenges,
    }
  } catch {
    return buildSeedState()
  }
}

function App() {
  const [tab, setTab] = useState<TabKey>('profile')
  const [state, setState] = useState<AppState>(() => loadState())
  const [isGenerating, setIsGenerating] = useState(false)
  const [goalDraft, setGoalDraft] = useState({ title: '', metric: 'weight' as GoalMetric, current: 0, target: 0, deadline: '2026-10-01' })
  const [logDraft, setLogDraft] = useState({
    date: new Date().toISOString().slice(0, 10),
    sessionType: 'strength' as WorkoutLog['sessionType'],
    durationMin: 45,
    intensityRpe: 7,
    caloriesBurned: 320,
    notes: '',
  })
  const [postText, setPostText] = useState('')
  const [feedQuery, setFeedQuery] = useState('')
  const deferredQuery = useDeferredValue(feedQuery)

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  }, [state])

  const analytics = getWeeklyAnalytics(state.logs)
  const goalAverage = state.goals.length
    ? Math.round(state.goals.reduce((sum, item) => sum + calculateGoalProgress(item), 0) / state.goals.length)
    : 0
  const filteredPosts = deferredQuery.trim()
    ? state.posts.filter((post) => {
        const q = deferredQuery.toLowerCase()
        return post.content.toLowerCase().includes(q) || post.author.toLowerCase().includes(q) || post.tags.join(' ').toLowerCase().includes(q)
      })
    : state.posts

  function updateProfile<K extends keyof UserProfile>(field: K, value: UserProfile[K]) {
    setState((prev) => ({ ...prev, profile: { ...prev.profile, [field]: value } }))
  }

  function regeneratePlans() {
    setIsGenerating(true)
    startTransition(() => {
      const trainingPlan = generateTrainingPlan(state.profile)
      const nutritionPlan = generateNutritionPlan(state.profile)
      const standards = getStandardApplications(state.profile, trainingPlan, nutritionPlan)
      setState((prev) => ({ ...prev, trainingPlan, nutritionPlan, standards }))
      setIsGenerating(false)
    })
  }

  function addGoal() {
    if (!goalDraft.title.trim()) return
    const unit = metricUnits[goalDraft.metric]
    const next: GoalItem = {
      id: uid('goal'),
      title: goalDraft.title.trim(),
      metric: goalDraft.metric,
      currentValue: goalDraft.current,
      targetValue: goalDraft.target,
      unit,
      deadline: goalDraft.deadline,
    }
    setState((prev) => ({ ...prev, goals: [next, ...prev.goals] }))
    setGoalDraft((prev) => ({ ...prev, title: '', current: 0, target: 0 }))
  }

  function addLog() {
    const next: WorkoutLog = { id: uid('log'), completed: true, ...logDraft }
    setState((prev) => ({ ...prev, logs: [next, ...prev.logs] }))
    setLogDraft((prev) => ({ ...prev, notes: '' }))
  }

  function toggleLog(logId: string) {
    setState((prev) => ({
      ...prev,
      logs: prev.logs.map((log) => (log.id === logId ? { ...log, completed: !log.completed } : log)),
    }))
  }

  function publishPost() {
    if (!postText.trim()) return
    const next: SocialPost = {
      id: uid('post'),
      author: state.profile.name || 'You',
      content: postText.trim(),
      tags: [state.profile.goalType, 'community'],
      likes: 0,
      comments: 0,
      createdAt: new Date().toISOString(),
    }
    setState((prev) => ({ ...prev, posts: [next, ...prev.posts] }))
    setPostText('')
  }

  function likePost(postId: string) {
    setState((prev) => ({
      ...prev,
      posts: prev.posts.map((post) => (post.id === postId ? { ...post, likes: post.likes + 1 } : post)),
    }))
  }

  function toggleChallenge(idToToggle: string) {
    setState((prev) => ({
      ...prev,
      challenges: prev.challenges.map((item) =>
        item.id === idToToggle
          ? { ...item, joined: !item.joined, participants: item.joined ? item.participants - 1 : item.participants + 1 }
          : item,
      ),
    }))
  }

  return (
    <div className="app">
      <header className="panel hero">
        <p className="kicker">Goal-Driven Fitness</p>
        <h1>Plan training by standards, execute by data.</h1>
        <div className="hero-metrics">
          <span>{state.profile.daysPerWeek} sessions/week</span>
          <span>{analytics.totalDuration} min (7d)</span>
          <span>{goalAverage}% goal progress</span>
        </div>
      </header>

      <nav className="panel tabs">
        {tabs.map((item) => (
          <button key={item.key} type="button" className={item.key === tab ? 'tab active' : 'tab'} onClick={() => setTab(item.key)}>
            {item.label}
          </button>
        ))}
      </nav>

      {tab === 'profile' && (
        <section className="panel section">
          <div className="topline"><h2>User Profile & Goal List</h2><button type="button" onClick={regeneratePlans}>{isGenerating ? 'Generating...' : 'Regenerate Plans'}</button></div>
          <div className="grid two">
            <article className="card">
              <h3>Profile Inputs</h3>
              <div className="form">
                <label>Name<input value={state.profile.name} onChange={(e) => updateProfile('name', e.target.value)} /></label>
                <label>Age<input type="number" value={state.profile.age} onChange={(e) => updateProfile('age', Number(e.target.value))} /></label>
                <label>Height(cm)<input type="number" value={state.profile.heightCm} onChange={(e) => updateProfile('heightCm', Number(e.target.value))} /></label>
                <label>Weight(kg)<input type="number" value={state.profile.weightKg} onChange={(e) => updateProfile('weightKg', Number(e.target.value))} /></label>
                <label>Goal<select value={state.profile.goalType} onChange={(e) => updateProfile('goalType', e.target.value as UserProfile['goalType'])}><option value="fat_loss">Fat Loss</option><option value="muscle_gain">Muscle Gain</option><option value="endurance">Endurance</option><option value="health">Health</option></select></label>
                <label>Level<select value={state.profile.level} onChange={(e) => updateProfile('level', e.target.value as UserProfile['level'])}><option value="beginner">Beginner</option><option value="intermediate">Intermediate</option><option value="advanced">Advanced</option></select></label>
                <label>Equipment<select value={state.profile.equipment} onChange={(e) => updateProfile('equipment', e.target.value as UserProfile['equipment'])}><option value="bodyweight">Bodyweight</option><option value="home_gym">Home Gym</option><option value="full_gym">Full Gym</option></select></label>
                <label>Days/week<input type="number" min={2} max={7} value={state.profile.daysPerWeek} onChange={(e) => updateProfile('daysPerWeek', Number(e.target.value))} /></label>
                <label>Recovery<select value={state.profile.recovery} onChange={(e) => updateProfile('recovery', e.target.value as UserProfile['recovery'])}><option value="low">Low</option><option value="medium">Medium</option><option value="high">High</option></select></label>
                <label>Sleep(h)<input type="number" step="0.1" value={state.profile.sleepHours} onChange={(e) => updateProfile('sleepHours', Number(e.target.value))} /></label>
              </div>
            </article>
            <article className="card">
              <h3>Goals</h3>
              <div className="goal-create">
                <input placeholder="Goal title" value={goalDraft.title} onChange={(e) => setGoalDraft((p) => ({ ...p, title: e.target.value }))} />
                <select value={goalDraft.metric} onChange={(e) => setGoalDraft((p) => ({ ...p, metric: e.target.value as GoalMetric }))}><option value="weight">Weight</option><option value="body_fat">Body Fat</option><option value="strength">Strength</option><option value="distance">Distance</option><option value="consistency">Consistency</option></select>
                <input type="number" placeholder="Current" value={goalDraft.current} onChange={(e) => setGoalDraft((p) => ({ ...p, current: Number(e.target.value) }))} />
                <input type="number" placeholder="Target" value={goalDraft.target} onChange={(e) => setGoalDraft((p) => ({ ...p, target: Number(e.target.value) }))} />
                <input type="date" value={goalDraft.deadline} onChange={(e) => setGoalDraft((p) => ({ ...p, deadline: e.target.value }))} />
                <button type="button" onClick={addGoal}>Add Goal</button>
              </div>
              {state.goals.map((goal) => {
                const p = calculateGoalProgress(goal)
                return <div key={goal.id} className="goal"><div className="row"><strong>{goal.title}</strong><span>{p}%</span></div><p>{goal.currentValue}{goal.unit} to {goal.targetValue}{goal.unit} by {goal.deadline}</p><div className="track"><div className="fill" style={{ width: `${p}%` }} /></div></div>
              })}
            </article>
          </div>
        </section>
      )}

      {tab === 'training' && <section className="panel section"><h2>Training Plan (International Standards Applied)</h2><div className="quick"><span>Split: {state.trainingPlan.split}</span><span>Sets/muscle: {state.trainingPlan.weeklyResistanceSetsPerMuscle}</span><span>Cardio: {state.trainingPlan.weeklyModerateCardioMinutes} min/week</span></div><div className="grid two"><article className="card">{state.trainingPlan.days.map((d) => <div key={d.day} className="day"><div className="row"><strong>{d.day} {d.focus}</strong><span>{d.strengthMinutes + d.cardioMinutes + d.mobilityMinutes}m</span></div><p>Strength {d.strengthMinutes}m · Cardio {d.cardioMinutes}m · Mobility {d.mobilityMinutes}m</p><ul>{d.exercises.map((e) => <li key={`${d.day}-${e.name}`}>{e.name} · {e.sets} x {e.reps} · rest {e.restSec}s</li>)}</ul></div>)}</article><article className="card">{state.standards.map((s) => <div key={s.standard} className="standard"><strong>{s.standard}</strong><p>{s.principle}</p><p className="rule">{s.appliedRule}</p></div>)}</article></div></section>}

      {tab === 'nutrition' && <section className="panel section"><h2>Nutrition Plan</h2><div className="quick"><span>{state.nutritionPlan.dailyKcal} kcal/day</span><span>Protein {state.nutritionPlan.proteinG}g</span><span>Carbs {state.nutritionPlan.carbsG}g</span><span>Fat {state.nutritionPlan.fatG}g</span></div><div className="grid two"><article className="card">{state.nutritionPlan.meals.map((meal) => <div key={meal.label} className="meal"><div className="row"><strong>{meal.label}</strong><span>{meal.kcal} kcal</span></div><p>{meal.foods.join(' · ')}</p><p className="rule">P/C/F {meal.proteinG}/{meal.carbsG}/{meal.fatG}g</p></div>)}</article><article className="card"><h3>Guidance</h3><p>Hydration target: {state.nutritionPlan.hydrationLiters}L/day</p><ul>{state.nutritionPlan.notes.map((n) => <li key={n}>{n}</li>)}</ul></article></div></section>}

      {tab === 'logs' && <section className="panel section"><h2>Workout Logs & Check-in</h2><div className="grid two"><article className="card"><div className="form"><label>Date<input type="date" value={logDraft.date} onChange={(e) => setLogDraft((p) => ({ ...p, date: e.target.value }))} /></label><label>Type<select value={logDraft.sessionType} onChange={(e) => setLogDraft((p) => ({ ...p, sessionType: e.target.value as WorkoutLog['sessionType'] }))}><option value="strength">Strength</option><option value="cardio">Cardio</option><option value="mobility">Mobility</option><option value="mixed">Mixed</option></select></label><label>Duration<input type="number" value={logDraft.durationMin} onChange={(e) => setLogDraft((p) => ({ ...p, durationMin: Number(e.target.value) }))} /></label><label>RPE<input type="number" value={logDraft.intensityRpe} onChange={(e) => setLogDraft((p) => ({ ...p, intensityRpe: Number(e.target.value) }))} /></label><label>Calories<input type="number" value={logDraft.caloriesBurned} onChange={(e) => setLogDraft((p) => ({ ...p, caloriesBurned: Number(e.target.value) }))} /></label><label className="wide">Notes<textarea rows={2} value={logDraft.notes} onChange={(e) => setLogDraft((p) => ({ ...p, notes: e.target.value }))} /></label></div><button type="button" onClick={addLog}>Add Log</button></article><article className="card table-card"><table><thead><tr><th>Date</th><th>Type</th><th>Duration</th><th>RPE</th><th>Status</th></tr></thead><tbody>{state.logs.map((log) => <tr key={log.id}><td>{log.date}</td><td>{log.sessionType}</td><td>{log.durationMin}m</td><td>{log.intensityRpe}</td><td><button type="button" onClick={() => toggleLog(log.id)}>{log.completed ? 'Done' : 'Pending'}</button></td></tr>)}</tbody></table></article></div></section>}

      {tab === 'analytics' && <section className="panel section"><h2>Data Analytics</h2><div className="quick"><span>Total 7d: {analytics.totalDuration}m</span><span>Completion: {analytics.completionRate}%</span><span>Avg RPE: {analytics.avgRpe}</span><span>Goal avg: {goalAverage}%</span></div><div className="grid two"><article className="card">{analytics.bars.map((b) => <div key={b.date} className="bar-row"><span>{b.date}</span><div className="bar"><div className="bar-fill" style={{ width: `${b.pct}%` }} /></div><strong>{b.duration}m</strong></div>)}</article><article className="card">{state.goals.map((g) => { const p = calculateGoalProgress(g); return <div key={`an-${g.id}`} className="goal"><div className="row"><strong>{g.title}</strong><span>{p}%</span></div><div className="track"><div className="fill" style={{ width: `${p}%` }} /></div></div> })}</article></div></section>}

      {tab === 'community' && <section className="panel section"><h2>Community & Challenges</h2><div className="grid two"><article className="card">{state.challenges.map((c) => <div key={c.id} className="standard"><strong>{c.title}</strong><p>{c.description}</p><p className="rule">{c.participants} participants · {c.deadline}</p><button type="button" onClick={() => toggleChallenge(c.id)}>{c.joined ? 'Leave' : 'Join'}</button></div>)}</article><article className="card"><textarea rows={2} placeholder="Share progress..." value={postText} onChange={(e) => setPostText(e.target.value)} /><div className="row"><input value={feedQuery} placeholder="Filter feed" onChange={(e) => setFeedQuery(e.target.value)} /><button type="button" onClick={publishPost}>Publish</button></div>{filteredPosts.map((p) => <div key={p.id} className="post"><div className="row"><strong>{p.author}</strong><span>{p.createdAt.slice(0, 10)}</span></div><p>{p.content}</p><p className="rule">#{p.tags.join(' #')}</p><div className="row"><span>{p.likes} likes · {p.comments} comments</span><button type="button" onClick={() => likePost(p.id)}>Like</button></div></div>)}</article></div></section>}
    </div>
  )
}

export default App
