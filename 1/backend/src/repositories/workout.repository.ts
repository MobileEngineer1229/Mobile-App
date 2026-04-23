import { database } from '../config/database';

export interface Workout {
  id: number;
  title: string;
  description: string;
  trimester: number; // 1, 2, or 3
  duration_minutes: number;
  difficulty_level: string; // 'beginner', 'intermediate', 'advanced'
  video_url?: string;
  thumbnail_url?: string;
  instructions: string;
  benefits: string[];
  precautions: string[];
  created_at: Date;
  updated_at: Date;
}

export class WorkoutRepository {
  async findByTrimester(trimester: number): Promise<Workout[]> {
    const result = await database.query(
      'SELECT * FROM pregnancy_workouts WHERE trimester = $1 ORDER BY title ASC',
      [trimester]
    );
    return result.rows;
  }

  async findAll(): Promise<Workout[]> {
    const result = await database.query('SELECT * FROM pregnancy_workouts ORDER BY trimester ASC, title ASC');
    return result.rows;
  }

  async findById(id: number): Promise<Workout | null> {
    const result = await database.query('SELECT * FROM pregnancy_workouts WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async create(workoutData: Partial<Workout>): Promise<Workout> {
    const result = await database.query(
      `INSERT INTO pregnancy_workouts 
       (title, description, trimester, duration_minutes, difficulty_level, video_url, 
        thumbnail_url, instructions, benefits, precautions)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       RETURNING *`,
      [
        workoutData.title,
        workoutData.description,
        workoutData.trimester,
        workoutData.duration_minutes,
        workoutData.difficulty_level,
        workoutData.video_url || null,
        workoutData.thumbnail_url || null,
        workoutData.instructions,
        workoutData.benefits || [],
        workoutData.precautions || [],
      ]
    );
    return result.rows[0];
  }
}
