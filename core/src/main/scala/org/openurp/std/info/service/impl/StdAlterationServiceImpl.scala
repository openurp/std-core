/*
 * Copyright (C) 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openurp.std.info.service.impl

import org.beangle.commons.json.{Json, JsonObject}
import org.beangle.data.dao.EntityDao
import org.openurp.base.edu.model.{Major, MajorDirection}
import org.openurp.base.hr.model.Teacher
import org.openurp.base.model.{Campus, Department}
import org.openurp.base.service.SemesterService
import org.openurp.base.std.model.*
import org.openurp.code.std.model.StudentStatus
import org.openurp.std.alter.config.AlterConfig
import org.openurp.std.alter.model.{AlterMeta, StdAlterApply, StdAlteration, StdAlterationItem}
import org.openurp.std.info.service.StdAlterationService

import java.time.{LocalDate, ZoneId}

class StdAlterationServiceImpl extends StdAlterationService {

  var entityDao: EntityDao = _

  var semesterService: SemesterService = _

  override def apply(alteration: StdAlteration, std: Student): String = {
    var target = std.states.find(x => !alteration.alterOn.isBefore(x.beginOn) && !alteration.alterOn.isAfter(x.endOn))
    val alterConfig = entityDao.findBy(classOf[AlterConfig], "alterType", alteration.alterType).head
    if (target.isEmpty) {
      if (alterConfig.alterEndOn) {
        target = std.states.sortBy(_.endOn).lastOption
      }
    }

    target match
      case None => "无法进行异动，找不到对应的学籍状态"
      case Some(t) =>
        val state =
          if (alterConfig.alterEndOn) {
            //预计离校时间从，预计毕业时间和变动时间中推测
            var endOn = alteration.getItem(AlterMeta.EndOn) match {
              case None =>
                if (alterConfig.alterGraduateOn) {
                  alteration.getItem(AlterMeta.GraduateOn) match
                    case None => alteration.alterOn
                    case Some(i) => LocalDate.parse(i.newvalue.get)
                } else {
                  alteration.alterOn
                }
              case Some(i) => LocalDate.parse(i.newvalue.get)
            }
            if endOn.isAfter(std.maxEndOn) then endOn = std.maxEndOn

            std.endOn = endOn
            t.endOn = endOn
            generateState(t, alteration.alterOn, alterConfig, alteration)
          } else {
            generateState(t, alteration.alterOn, alterConfig, alteration)
          }

        alteration.items foreach { item =>
          item.meta match {
            case AlterMeta.Grade => state.grade = entityDao.get(classOf[Grade], item.newvalue.get.toLong)
            case AlterMeta.Department => state.department = entityDao.get(classOf[Department], item.newvalue.get.toInt)
            case AlterMeta.Major => state.major = entityDao.get(classOf[Major], item.newvalue.get.toLong)
            case AlterMeta.Direction =>
              item.newvalue match
                case None => state.direction = None
                case Some(id) => state.direction = Some(entityDao.get(classOf[MajorDirection], id.toLong))
            case AlterMeta.Squad =>
              item.newvalue match
                case None => state.squad = None
                case Some(id) => state.squad = Some(entityDao.get(classOf[Squad], id.toLong))
            case AlterMeta.Inschool => state.inschool = item.newvalue.get.toBoolean
            case AlterMeta.Status => state.status = entityDao.get(classOf[StudentStatus], item.newvalue.get.toInt)
            case AlterMeta.Campus => state.campus = entityDao.get(classOf[Campus], item.newvalue.get.toInt)
            case AlterMeta.EndOn => state.std.endOn = LocalDate.parse(item.newvalue.get)
            case AlterMeta.Tutor => std.updateTutors(List(entityDao.get(classOf[Teacher], item.newvalue.get.toLong)), Tutorship.Major)
            case AlterMeta.Advisor => std.updateTutors(List(entityDao.get(classOf[Teacher], item.newvalue.get.toLong)), Tutorship.Thesis)
            case AlterMeta.GraduateOn => std.graduateOn = LocalDate.parse(item.newvalue.get)
          }
        }
        //如果异动信息中没有涉及是否在校和学籍状态，则按照异动配置来设置。
        if (alteration.getItem(AlterMeta.Inschool).isEmpty) {
          state.inschool = alterConfig.inschool
        }
        if (alteration.getItem(AlterMeta.Status).isEmpty) {
          state.status = alterConfig.status
        }
        state.std.calcCurrentState()
        entityDao.saveOrUpdate(state, target, state.std)
        ""
  }

  override def approve(apply: StdAlterApply): Unit = {
    if (apply.alterFrom.isEmpty) {
      apply.alterFrom = Some(apply.lastStep.get.auditAt.get.atZone(ZoneId.systemDefault()).toLocalDate)
    }
    entityDao.saveOrUpdate(apply)
    val alterFrom = apply.alterFrom.get
    val semester = semesterService.get(apply.std.project, alterFrom)
    val std = apply.std
    val alt = new StdAlteration(std, apply.alterType, semester, alterFrom)
    val data = Json.parseObject(apply.alterDataJson)
    data.keys foreach { key =>
      val meta = AlterMeta.of(key)
      val idata = data(key).asInstanceOf[JsonObject]
      val i = new StdAlterationItem(meta, idata.getString("oldvalue"), idata.getString("oldtext"), idata.getString("newvalue"), idata.getString("newtext"))
      alt.addItem(i)
    }
    entityDao.saveOrUpdate(alt)
    this.apply(alt, std)
  }

  private def generateState(state: StudentState, newBeginOn: LocalDate, alterConfig: AlterConfig, alteration: StdAlteration): StudentState = { // 向后切
    if (newBeginOn == state.beginOn) {
      state
    } else {
      val newState = new StudentState
      newState.std = state.std
      newState.beginOn = newBeginOn
      newState.endOn = state.endOn //保留被截断状态的结束时间
      newState.grade = state.grade
      newState.department = state.department
      newState.major = state.major
      newState.direction = state.direction
      newState.squad = state.squad
      newState.status = state.status
      newState.campus = state.campus
      newState.inschool = state.inschool
      newState.remark = Some(alteration.reason.map(_.name).getOrElse(alteration.alterType.name))
      state.endOn = newBeginOn.minusDays(1)
      state.std.states += newState
      newState
    }
  }
}
