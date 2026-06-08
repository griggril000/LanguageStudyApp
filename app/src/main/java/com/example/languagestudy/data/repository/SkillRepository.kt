package com.example.languagestudy.data.repository

import com.example.languagestudy.data.local.dao.SkillDao
import com.example.languagestudy.data.local.entity.SkillEntity
import kotlinx.coroutines.flow.Flow

class SkillRepository(private val skillDao: SkillDao) {
    val allSkills: Flow<List<SkillEntity>> = skillDao.getAllSkills()

    suspend fun insert(skill: SkillEntity) {
        skillDao.insertSkill(skill)
    }

    suspend fun update(skill: SkillEntity) {
        skillDao.updateSkill(skill)
    }
}
