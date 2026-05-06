import { Character } from '../../../db/mongo/models';
import type { ICharacter } from '../../../db/mongo/models';

const MAX_SLOTS = 5;

export class CharacterManager {
  // Give a new player their default character copies (cloned from global templates)
  async initPlayerCharacters(playerId: string): Promise<ICharacter[]> {
    const existing = await Character.find({ playerId }).lean() as unknown as ICharacter[];
    if (existing.length > 0) return existing;

    const templates = await Character.find({
      playerId: null,
      'unlock.isDefault': true,
      isActive: true,
    }).lean() as unknown as ICharacter[];

    const copies = await Character.insertMany(
      templates.slice(0, MAX_SLOTS).map(t => ({
        ...t,
        _id: undefined,
        playerId,
        progression: { level: 1, experience: 0, prestigeLevel: 0 },
      }))
    );

    return copies as unknown as ICharacter[];
  }

  async getCharacters(playerId: string): Promise<ICharacter[]> {
    const chars = await Character.find({ playerId, isActive: true }).lean() as unknown as ICharacter[];
    if (chars.length === 0) return this.initPlayerCharacters(playerId);
    return chars;
  }

  async getCharacter(playerId: string, characterId: string): Promise<ICharacter | null> {
    return Character.findOne({ _id: characterId, playerId, isActive: true }).lean() as Promise<ICharacter | null>;
  }

  // Unlock a non-default character (deducts coins — caller must verify balance)
  async unlockCharacter(playerId: string, templateId: string): Promise<ICharacter | { error: string }> {
    const existing = await Character.findOne({ playerId, templateId });
    if (existing) return { error: 'Already owned' };

    const owned = await Character.countDocuments({ playerId, isActive: true });
    if (owned >= MAX_SLOTS) return { error: `Character slots full (max ${MAX_SLOTS})` };

    const template = await Character.findOne({ playerId: null, templateId, isActive: true }).lean() as ICharacter | null;
    if (!template) return { error: 'Character not found' };

    const copy = await Character.create({
      ...template,
      _id: undefined,
      playerId,
      progression: { level: 1, experience: 0, prestigeLevel: 0 },
    });

    return copy as unknown as ICharacter;
  }

  // Award XP and level up if threshold reached
  async addExperience(playerId: string, characterId: string, xp: number): Promise<ICharacter | null> {
    const char = await Character.findOne({ _id: characterId, playerId });
    if (!char) return null;

    char.progression.experience += xp;

    // XP to next level = current level × 100
    while (char.progression.experience >= char.progression.level * 100) {
      char.progression.experience -= char.progression.level * 100;
      char.progression.level = Math.min(char.progression.level + 1, 50);
    }

    await char.save();
    return char.toObject() as ICharacter;
  }

  // Apply a cosmetic change (skin, bomb skin, etc.)
  async applyCosmetic(
    playerId: string,
    characterId: string,
    field: keyof ICharacter['cosmetics'],
    value: string,
  ): Promise<ICharacter | null> {
    const char = await Character.findOneAndUpdate(
      { _id: characterId, playerId },
      { $set: { [`cosmetics.${String(field)}`]: value } },
      { new: true },
    ).lean();
    return char as ICharacter | null;
  }

  // Get all available templates (for the shop/unlock screen)
  async getTemplates(): Promise<ICharacter[]> {
    return Character.find({ playerId: null, isActive: true }).lean() as unknown as Promise<ICharacter[]>;
  }
}
