import { database } from '../config/database';
import crypto from 'crypto';

export class FamilySharingRepository {
  // ─── Invitations ──────────────────────────────────────────────────────────

  async createInvitation(babyId: number, inviterId: number, inviteeEmail: string, role: string) {
    const code = crypto.randomBytes(32).toString('hex');
    const row = await database.query(
      `INSERT INTO family_invitations (baby_id, inviter_id, invitee_email, invite_code, role)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [babyId, inviterId, inviteeEmail.toLowerCase(), code, role]
    );
    return row.rows[0];
  }

  async findInvitationByCode(code: string) {
    const row = await database.query(
      `SELECT fi.*, b.name AS baby_name, u.full_name AS inviter_name
       FROM family_invitations fi
       JOIN babies b ON b.id = fi.baby_id
       JOIN users  u ON u.id = fi.inviter_id
       WHERE fi.invite_code = $1`,
      [code]
    );
    return row.rows[0] ?? null;
  }

  async listInvitationsForBaby(babyId: number) {
    const rows = await database.query(
      `SELECT id, invitee_email, role, status, expires_at, created_at
       FROM family_invitations
       WHERE baby_id = $1
       ORDER BY created_at DESC`,
      [babyId]
    );
    return rows.rows;
  }

  async updateInvitationStatus(id: number, status: string) {
    const row = await database.query(
      `UPDATE family_invitations SET status = $1 WHERE id = $2 RETURNING *`,
      [status, id]
    );
    return row.rows[0] ?? null;
  }

  async deleteInvitation(id: number, inviterId: number) {
    const row = await database.query(
      `DELETE FROM family_invitations WHERE id = $1 AND inviter_id = $2 RETURNING id`,
      [id, inviterId]
    );
    return row.rowCount > 0;
  }

  // ─── Members ──────────────────────────────────────────────────────────────

  async addMember(babyId: number, userId: number, role: string, invitedBy: number) {
    const row = await database.query(
      `INSERT INTO family_members (baby_id, user_id, role, invited_by)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (baby_id, user_id) DO UPDATE SET role = EXCLUDED.role
       RETURNING *`,
      [babyId, userId, role, invitedBy]
    );
    return row.rows[0];
  }

  async listMembersForBaby(babyId: number) {
    const rows = await database.query(
      `SELECT fm.id, fm.user_id, fm.role, fm.joined_at,
              u.full_name, u.email, u.profile_picture_url,
              inv.full_name AS invited_by_name
       FROM family_members fm
       JOIN users u ON u.id = fm.user_id
       LEFT JOIN users inv ON inv.id = fm.invited_by
       WHERE fm.baby_id = $1
       ORDER BY fm.joined_at`,
      [babyId]
    );
    return rows.rows;
  }

  async listBabiesForUser(userId: number) {
    const rows = await database.query(
      `SELECT b.id, b.name, b.birth_date, b.gender, b.profile_picture_url,
              fm.role, fm.joined_at,
              u.full_name AS owner_name
       FROM family_members fm
       JOIN babies b ON b.id = fm.baby_id
       JOIN users  u ON u.id = b.user_id
       WHERE fm.user_id = $1
       ORDER BY fm.joined_at`,
      [userId]
    );
    return rows.rows;
  }

  async removeMember(babyId: number, userId: number, requesterId: number) {
    // Owner (baby.user_id) or the member themselves can remove
    const row = await database.query(
      `DELETE FROM family_members
       WHERE baby_id = $1 AND user_id = $2
         AND (
           $3 IN (SELECT user_id FROM babies WHERE id = $1)
           OR $3 = $2
         )
       RETURNING id`,
      [babyId, userId, requesterId]
    );
    return (row.rowCount ?? 0) > 0;
  }

  async updateMemberRole(babyId: number, userId: number, role: string, ownerId: number) {
    // Only owner can change roles
    const row = await database.query(
      `UPDATE family_members fm
       SET role = $3
       FROM babies b
       WHERE fm.baby_id = $1 AND fm.user_id = $2
         AND b.id = $1 AND b.user_id = $4
       RETURNING fm.*`,
      [babyId, userId, role, ownerId]
    );
    return row.rows[0] ?? null;
  }

  async isMember(babyId: number, userId: number) {
    const row = await database.query(
      `SELECT role FROM family_members WHERE baby_id = $1 AND user_id = $2`,
      [babyId, userId]
    );
    return row.rows[0] ?? null;
  }

  async isOwner(babyId: number, userId: number) {
    const row = await database.query(
      `SELECT id FROM babies WHERE id = $1 AND user_id = $2`,
      [babyId, userId]
    );
    return row.rowCount > 0;
  }
}
