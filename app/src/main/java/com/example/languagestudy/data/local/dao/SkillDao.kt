package com.example.languagestudy.data.local.dao

import androidx.room.*
import com.example.languagestudy.data.local.entity.SkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY priority ASC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity)

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Delete
    suspend fun deleteSkill(skill: SkillEntity)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkillById(id: String)
}
