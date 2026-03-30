export type GoalType = 'fat_loss' | 'muscle_gain' | 'endurance' | 'health'

export type ExperienceLevel = 'beginner' | 'intermediate' | 'advanced'

export type EquipmentAccess = 'bodyweight' | 'home_gym' | 'full_gym'

export type RecoveryLevel = 'low' | 'medium' | 'high'

export type Sex = 'male' | 'female'

export type GoalMetric = 'weight' | 'body_fat' | 'strength' | 'distance' | 'consistency'

export interface UserProfile {
  name: string
  age: number
  sex: Sex
  heightCm: number
  weightKg: number
  bodyFatPct: number
  goalType: GoalType
  level: ExperienceLevel
  equipment: EquipmentAccess
  daysPerWeek: number
  recovery: RecoveryLevel
  sleepHours: number
  stepsPerDay: number
  injuries: string
}

export interface GoalItem {
  id: string
  title: string
  metric: GoalMetric
  targetValue: number
  currentValue: number
  unit: string
  deadline: string
}

export interface ExercisePrescription {
  name: string
  sets: number
  reps: string
  restSec: number
  intensityCue: string
}

export interface WorkoutDayPlan {
  day: string
  focus: string
  strengthMinutes: number
  cardioMinutes: number
  mobilityMinutes: number
  exercises: ExercisePrescription[]
}

export interface TrainingPlan {
  split: string
  weeklyResistanceSetsPerMuscle: number
  weeklyModerateCardioMinutes: number
  progressionRule: string
  deloadRule: string
  generatedAt: string
  days: WorkoutDayPlan[]
}

export interface MealItem {
  label: string
  foods: string[]
  proteinG: number
  carbsG: number
  fatG: number
  kcal: number
}

export interface NutritionPlan {
  dailyKcal: number
  proteinG: number
  carbsG: number
  fatG: number
  hydrationLiters: number
  notes: string[]
  meals: MealItem[]
}

export interface WorkoutLog {
  id: string
  date: string
  sessionType: 'strength' | 'cardio' | 'mobility' | 'mixed'
  durationMin: number
  intensityRpe: number
  caloriesBurned: number
  completed: boolean
  notes: string
}

export interface SocialPost {
  id: string
  author: string
  content: string
  tags: string[]
  likes: number
  comments: number
  createdAt: string
}

export interface ChallengeItem {
  id: string
  title: string
  description: string
  participants: number
  joined: boolean
  deadline: string
}

export interface StandardApplication {
  standard: string
  principle: string
  appliedRule: string
}
