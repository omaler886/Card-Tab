import type {
  ChallengeItem,
  EquipmentAccess,
  ExperienceLevel,
  GoalItem,
  GoalType,
  NutritionPlan,
  RecoveryLevel,
  StandardApplication,
  TrainingPlan,
  UserProfile,
  WorkoutDayPlan,
  WorkoutLog,
} from './types'

const WEEK_DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

const EXERCISE_LIBRARY: Record<EquipmentAccess, Record<GoalType, string[]>> = {
  bodyweight: {
    fat_loss: [
      'Bodyweight Squat',
      'Push-up',
      'Reverse Lunge',
      'Plank Shoulder Tap',
      'Mountain Climber',
      'Glute Bridge',
    ],
    muscle_gain: [
      'Tempo Push-up',
      'Bulgarian Split Squat',
      'Pike Push-up',
      'Single-leg Hip Thrust',
      'Chin-up (Assisted if needed)',
      'Slow Eccentric Squat',
    ],
    endurance: [
      'Step-up',
      'Burpee (Scaled)',
      'Walking Lunge',
      'Jump Rope',
      'High Knee March',
      'Bear Crawl',
    ],
    health: [
      'Squat to Box',
      'Wall Push-up',
      'Bird Dog',
      'Dead Bug',
      'Hip Hinge Drill',
      'Sit-to-Stand',
    ],
  },
  home_gym: {
    fat_loss: [
      'Dumbbell Goblet Squat',
      'Dumbbell Bench Press',
      'One-arm Row',
      'Romanian Deadlift',
      'Kettlebell Swing',
      'Assault Bike Intervals',
    ],
    muscle_gain: [
      'Barbell Back Squat',
      'Barbell Bench Press',
      'Overhead Press',
      'Barbell Row',
      'Romanian Deadlift',
      'Pull-up',
    ],
    endurance: [
      'Dumbbell Thruster',
      'Kettlebell Swing',
      'Row Erg Sprint',
      'Farmer Carry',
      'Cycling Intervals',
      'Sled Push',
    ],
    health: [
      'Goblet Squat',
      'Machine Chest Press',
      'Cable Row',
      'Leg Curl',
      'Elliptical',
      'Mobility Flow',
    ],
  },
  full_gym: {
    fat_loss: [
      'Front Squat',
      'Incline Dumbbell Press',
      'Lat Pulldown',
      'Romanian Deadlift',
      'Cable Woodchop',
      'Treadmill Incline Walk',
    ],
    muscle_gain: [
      'Back Squat',
      'Bench Press',
      'Deadlift',
      'Weighted Pull-up',
      'Incline Press',
      'Seated Row',
    ],
    endurance: [
      'Row Erg',
      'Ski Erg',
      'Air Bike Intervals',
      'Walking Lunge',
      'Battle Rope',
      'Circuit Medley',
    ],
    health: [
      'Leg Press',
      'Machine Row',
      'Cable Press',
      'Hamstring Curl',
      'Bike Zone 2',
      'Mobility Circuit',
    ],
  },
}

const GOAL_CALORIE_ADJUSTMENT: Record<GoalType, number> = {
  fat_loss: -420,
  muscle_gain: 270,
  endurance: 120,
  health: 0,
}

const GOAL_CARDIO_TARGET: Record<GoalType, number> = {
  fat_loss: 220,
  muscle_gain: 120,
  endurance: 280,
  health: 170,
}

const LEVEL_SET_BASE: Record<ExperienceLevel, number> = {
  beginner: 10,
  intermediate: 14,
  advanced: 18,
}

const RECOVERY_MODIFIER: Record<RecoveryLevel, number> = {
  low: -3,
  medium: 0,
  high: 2,
}

function getActivityMultiplier(daysPerWeek: number, stepsPerDay: number): number {
  if (daysPerWeek <= 2 && stepsPerDay < 6000) return 1.3
  if (daysPerWeek <= 3 && stepsPerDay < 8000) return 1.45
  if (daysPerWeek <= 5 && stepsPerDay < 11000) return 1.6
  return 1.75
}

function getBmr(profile: UserProfile): number {
  // Mifflin-St Jeor equation.
  const base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age
  return profile.sex === 'male' ? base + 5 : base - 161
}

function round10(value: number): number {
  return Math.round(value / 10) * 10
}

function pickRepRange(goal: GoalType): string {
  if (goal === 'muscle_gain') return '6-12'
  if (goal === 'endurance') return '12-20'
  if (goal === 'fat_loss') return '8-15'
  return '8-12'
}

function getSplitName(daysPerWeek: number): string {
  if (daysPerWeek <= 3) return 'Full-body split'
  if (daysPerWeek <= 5) return 'Upper/Lower + Conditioning'
  return 'Push/Pull/Legs + Athletic days'
}

function buildDayFocus(index: number, daysPerWeek: number, goalType: GoalType): string {
  if (daysPerWeek <= 3) {
    const labels = ['Full-body A', 'Full-body B', 'Full-body C']
    return labels[index] ?? 'Full-body'
  }

  if (daysPerWeek <= 5) {
    const labels = ['Upper Strength', 'Lower Strength', 'Conditioning', 'Upper Hypertrophy', 'Lower + Core']
    return labels[index] ?? 'Conditioning'
  }

  const labels = [
    'Push Strength',
    'Pull Strength',
    'Leg Strength',
    'Conditioning Power',
    'Push Hypertrophy',
    'Pull Hypertrophy',
    'Aerobic Base',
  ]
  if (goalType === 'endurance' && labels[index] === 'Conditioning Power') return 'Threshold Conditioning'
  return labels[index] ?? 'Mixed'
}

function buildExercises(
  equipment: EquipmentAccess,
  goalType: GoalType,
  daysPerWeek: number,
  dayIndex: number,
  weeklySetsPerMuscle: number,
): WorkoutDayPlan['exercises'] {
  const library = EXERCISE_LIBRARY[equipment][goalType]
  const reps = pickRepRange(goalType)
  const setsPerExercise = Math.max(2, Math.round(weeklySetsPerMuscle / (daysPerWeek * 1.8)))
  const shifted = [...library.slice(dayIndex), ...library.slice(0, dayIndex)]
  return shifted.slice(0, 4).map((name, index) => ({
    name,
    sets: setsPerExercise + (index === 0 ? 1 : 0),
    reps,
    restSec: goalType === 'endurance' ? 45 : goalType === 'fat_loss' ? 60 : 90,
    intensityCue:
      goalType === 'muscle_gain'
        ? 'RPE 7-9, keep 1-3 reps in reserve'
        : goalType === 'endurance'
          ? 'RPE 6-8, sustainable pace'
          : 'RPE 6-8 with clean technique',
  }))
}

function buildTrainingDays(profile: UserProfile, weeklySetsPerMuscle: number, weeklyCardioMinutes: number): WorkoutDayPlan[] {
  const cardioPerSession = Math.round(weeklyCardioMinutes / profile.daysPerWeek)
  return WEEK_DAYS.slice(0, profile.daysPerWeek).map((day, index) => {
    const focus = buildDayFocus(index, profile.daysPerWeek, profile.goalType)
    const exercises = buildExercises(
      profile.equipment,
      profile.goalType,
      profile.daysPerWeek,
      index,
      weeklySetsPerMuscle,
    )

    return {
      day,
      focus,
      strengthMinutes: profile.goalType === 'endurance' ? 35 : 50,
      cardioMinutes: cardioPerSession + (focus.includes('Conditioning') ? 12 : 0),
      mobilityMinutes: profile.recovery === 'low' ? 18 : 12,
      exercises,
    }
  })
}

export function generateTrainingPlan(profile: UserProfile): TrainingPlan {
  const baseSets = LEVEL_SET_BASE[profile.level] + RECOVERY_MODIFIER[profile.recovery]
  const weeklySetsPerMuscle = Math.max(8, baseSets)
  const goalCardio = GOAL_CARDIO_TARGET[profile.goalType]
  const sleepPenalty = profile.sleepHours < 7 ? -20 : 0
  const weeklyModerateCardioMinutes = Math.max(90, goalCardio + sleepPenalty)

  return {
    split: getSplitName(profile.daysPerWeek),
    weeklyResistanceSetsPerMuscle: weeklySetsPerMuscle,
    weeklyModerateCardioMinutes,
    progressionRule:
      'If last session average RPE <= 7 and technique stable, increase load 2.5-5% or add 1 rep next week.',
    deloadRule:
      'Every 5th week reduce load volume ~30%, prioritize mobility and sleep restoration.',
    generatedAt: new Date().toISOString(),
    days: buildTrainingDays(profile, weeklySetsPerMuscle, weeklyModerateCardioMinutes),
  }
}

export function generateNutritionPlan(profile: UserProfile): NutritionPlan {
  const bmr = getBmr(profile)
  const tdee = bmr * getActivityMultiplier(profile.daysPerWeek, profile.stepsPerDay)
  const targetKcal = Math.max(1400, round10(tdee + GOAL_CALORIE_ADJUSTMENT[profile.goalType]))

  const proteinMultiplier =
    profile.goalType === 'muscle_gain' ? 2.0 : profile.goalType === 'fat_loss' ? 2.1 : 1.7
  const proteinG = round10(profile.weightKg * proteinMultiplier)
  const fatG = round10(profile.weightKg * 0.8)
  const remainingCalories = targetKcal - (proteinG * 4 + fatG * 9)
  const carbsG = round10(Math.max(80, remainingCalories / 4))

  const meals = [
    {
      label: 'Breakfast',
      foods: ['Oats + Greek yogurt + berries', '2 eggs or tofu scramble'],
      proteinG: Math.round(proteinG * 0.3),
      carbsG: Math.round(carbsG * 0.3),
      fatG: Math.round(fatG * 0.25),
      kcal: Math.round(targetKcal * 0.3),
    },
    {
      label: 'Lunch',
      foods: ['Lean protein bowl', 'Rice/quinoa', 'Mixed vegetables + olive oil'],
      proteinG: Math.round(proteinG * 0.3),
      carbsG: Math.round(carbsG * 0.33),
      fatG: Math.round(fatG * 0.3),
      kcal: Math.round(targetKcal * 0.35),
    },
    {
      label: 'Dinner',
      foods: ['Fish/chicken/legumes', 'Potato or whole grain', 'Large salad'],
      proteinG: Math.round(proteinG * 0.3),
      carbsG: Math.round(carbsG * 0.27),
      fatG: Math.round(fatG * 0.35),
      kcal: Math.round(targetKcal * 0.3),
    },
    {
      label: 'Training Snack',
      foods: ['Banana + whey/soy isolate'],
      proteinG: Math.max(15, Math.round(proteinG * 0.1)),
      carbsG: Math.max(20, Math.round(carbsG * 0.1)),
      fatG: Math.max(5, Math.round(fatG * 0.1)),
      kcal: Math.round(targetKcal * 0.1),
    },
  ]

  return {
    dailyKcal: targetKcal,
    proteinG,
    carbsG,
    fatG,
    hydrationLiters: Math.max(2.2, Number((profile.weightKg * 0.035).toFixed(1))),
    notes: [
      'Prioritize 25-40g protein per meal for muscle protein synthesis support.',
      'Distribute carbs around training sessions for performance and recovery.',
      'Aim for 80% minimally processed food and 20% flexible intake.',
      'Keep sodium and potassium balanced, especially on high sweat days.',
    ],
    meals,
  }
}

export function getStandardApplications(profile: UserProfile, plan: TrainingPlan, nutrition: NutritionPlan): StandardApplication[] {
  return [
    {
      standard: 'WHO Physical Activity Guideline',
      principle: 'Adults should accumulate 150-300 min moderate or 75-150 min vigorous activity weekly.',
      appliedRule: `Cardio target set to ${plan.weeklyModerateCardioMinutes} min/week from goal + recovery profile.`,
    },
    {
      standard: 'ACSM Resistance Training Position Stand',
      principle: 'Train major muscle groups 2-3+ days/week with progressive overload.',
      appliedRule: `${profile.daysPerWeek} sessions/week and ${plan.weeklyResistanceSetsPerMuscle} sets per muscle group with progression and deload.`,
    },
    {
      standard: 'NSCA Training Volume Guidance',
      principle: 'Volume and intensity scale with training level and recovery capacity.',
      appliedRule: `Volume adapted by level (${profile.level}) and recovery (${profile.recovery}).`,
    },
    {
      standard: 'NASM OPT / Recovery Principles',
      principle: 'Include mobility, corrective focus, and periodized recovery to reduce injury risk.',
      appliedRule: `Each session includes ${plan.days[0]?.mobilityMinutes ?? 12} min mobility and a 5th-week deload.`,
    },
    {
      standard: 'USDA / Sports Nutrition Consensus',
      principle: 'Calorie target and macros should align with body mass, goal, and training demand.',
      appliedRule: `${nutrition.dailyKcal} kcal with P/C/F = ${nutrition.proteinG}/${nutrition.carbsG}/${nutrition.fatG}g.`,
    },
  ]
}

export function calculateGoalProgress(goal: GoalItem): number {
  if (goal.targetValue === goal.currentValue) return 100
  const totalDelta = Math.abs(goal.targetValue - goal.currentValue)
  const isDecreaseGoal = goal.metric === 'weight' || goal.metric === 'body_fat'
  const effectiveCurrent = isDecreaseGoal ? goal.currentValue - totalDelta * 0.45 : goal.currentValue + totalDelta * 0.45
  const range = Math.abs(goal.targetValue - goal.currentValue)
  const done = Math.abs(effectiveCurrent - goal.currentValue)
  return Math.max(5, Math.min(95, Math.round((done / range) * 100)))
}

export function getWeeklyAnalytics(logs: WorkoutLog[]) {
  const last7Days = new Map<string, number>()
  const today = new Date()
  for (let i = 0; i < 7; i += 1) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    const key = d.toISOString().slice(0, 10)
    last7Days.set(key, 0)
  }

  let completedCount = 0
  let intensitySum = 0
  let loggedIntensityCount = 0

  logs.forEach((log) => {
    if (last7Days.has(log.date)) {
      last7Days.set(log.date, (last7Days.get(log.date) ?? 0) + log.durationMin)
    }
    if (log.completed) completedCount += 1
    if (log.intensityRpe > 0) {
      intensitySum += log.intensityRpe
      loggedIntensityCount += 1
    }
  })

  const totalDuration = [...last7Days.values()].reduce((sum, value) => sum + value, 0)
  const completionRate = logs.length ? Math.round((completedCount / logs.length) * 100) : 0
  const avgRpe = loggedIntensityCount ? Number((intensitySum / loggedIntensityCount).toFixed(1)) : 0

  const bars = [...last7Days.entries()]
    .sort(([a], [b]) => (a < b ? -1 : 1))
    .map(([date, duration]) => ({
      date: date.slice(5),
      duration,
      pct: Math.min(100, Math.round((duration / 120) * 100)),
    }))

  return {
    totalDuration,
    completionRate,
    avgRpe,
    bars,
  }
}

export function defaultChallenges(): ChallengeItem[] {
  return [
    {
      id: 'challenge-8k',
      title: '8k Steps Consistency',
      description: 'Hit at least 8,000 steps for 21 days.',
      participants: 1380,
      joined: false,
      deadline: '2026-05-05',
    },
    {
      id: 'challenge-strength',
      title: 'Progressive Strength Cycle',
      description: 'Log 12 strength sessions in 4 weeks.',
      participants: 742,
      joined: false,
      deadline: '2026-04-28',
    },
    {
      id: 'challenge-hiit',
      title: 'Weekly HIIT Pair',
      description: 'Complete two interval workouts each week.',
      participants: 520,
      joined: false,
      deadline: '2026-05-12',
    },
  ]
}

export function defaultLogs(): WorkoutLog[] {
  const today = new Date()
  const list: WorkoutLog[] = []
  for (let i = 0; i < 6; i += 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - i)
    list.push({
      id: `seed-log-${i}`,
      date: date.toISOString().slice(0, 10),
      sessionType: i % 2 === 0 ? 'strength' : 'cardio',
      durationMin: i % 2 === 0 ? 58 : 40,
      intensityRpe: i % 2 === 0 ? 7 : 6,
      caloriesBurned: i % 2 === 0 ? 360 : 280,
      completed: true,
      notes: i % 2 === 0 ? 'Main lifts felt stable.' : 'Intervals completed at target pace.',
    })
  }
  return list
}
