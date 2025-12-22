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

import org.beangle.commons.logging.Logging
import org.beangle.data.dao.OqlBuilder
import org.beangle.data.orm.hibernate.AbstractDaoTask
import org.openurp.base.model.User
import org.openurp.base.std.model.{Graduate, Student}

import java.time.LocalDate

/** 自动修正学生的学籍状态
 */
class StudentStateFixer extends AbstractDaoTask, Logging {

  override def execute(): Unit = {
    //1. 根据时间修正学生的学籍状态
    fixState()
    // 2. 对未毕业已经过期的同学进行自动延长
    autoProlong()
    //3.同步学生用户结束日期
    syncUserEndOn()
  }

  /** 根据时间修正学生的学籍状态
   */
  private def fixState(): Unit = {
    val updateQl = s"update ${classOf[Student].getName} s set state=(select min(ss.id) from s.states ss where " +
      s"?1 between ss.beginOn and ss.endOn) where exists(from s.states ss where " +
      s"?1 between ss.beginOn and ss.endOn and ss!=s.state)"

    val updated = entityDao.executeUpdate(updateQl, LocalDate.now())
    if updated > 0 then logger.info(s"auto update ${updated} student state.")
  }

  /**
   * 对未毕业已经过期的同学进行自动延长
   * 但标记是否延期，仅仅延长时间。这样可以防止同学进行自动不在籍的状态。
   */
  private def autoProlong(): Unit = {
    val q = OqlBuilder.from(classOf[Student], "s")
    q.where("current_date between s.beginOn and s.maxEndOn") //还在最长年限内
    q.where("current_date not between s.state.beginOn and s.state.endOn") //目前已经不在时间范围内了
    q.where("s.state.inschool=true")
    q.where("not exists(from " + classOf[Graduate].getName + " g where g.std=s)") //没有毕业记录
    val stds = entityDao.search(q)
    if (stds.nonEmpty) {
      stds foreach { std =>
        std.state foreach { state =>
          var newEndOn = state.endOn.plusYears(1)
          if (newEndOn.isAfter(std.maxEndOn)) newEndOn = std.maxEndOn
          state.endOn = newEndOn
          var remark = state.remark.getOrElse("")
          if (!remark.contains("自动延长")) {
            remark += s" ${LocalDate.now}自动延长"
          }
          if (std.endOn.isBefore(newEndOn)) {
            std.endOn = newEndOn
          }
          state.remark = Some(remark)
        }
      }
      entityDao.saveOrUpdate(stds)
      logger.info(s"自动延长了 ${stds.size} 学生的有效期.")
    }
  }

  /** 同步学生的endOn到用户表
   */
  private def syncUserEndOn(): Unit = {
    val updateQl = s"update ${classOf[User].getName} u set u.endOn=(select max(std.endOn) from ${classOf[Student].getName} std where " +
      s"std.user=u) where exists(from ${classOf[Student].getName} std where std.user=u and u.endOn < std.endOn)"
    val updated = entityDao.executeUpdate(updateQl)
    if updated > 0 then logger.info(s"Auto sync  ${updated} students user endOn.")
  }
}
