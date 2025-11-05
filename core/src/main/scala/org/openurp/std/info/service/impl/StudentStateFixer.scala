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
import org.beangle.data.orm.hibernate.AbstractDaoTask
import org.openurp.base.std.model.Student

import java.time.LocalDate

/** 自动修正学生的学籍状态
 */
class StudentStateFixer extends AbstractDaoTask, Logging {

  override def execute(): Unit = {
    val updateQl = s"update ${classOf[Student].getName} s set state=(select min(ss.id) from s.states ss where " +
      s"?1 between ss.beginOn and ss.endOn) where exists(from s.states ss where " +
      s"?1 between ss.beginOn and ss.endOn and ss!=s.state)"

    val updated = entityDao.executeUpdate(updateQl, LocalDate.now())
    if updated > 0 then logger.info(s"auto update ${updated} student state.")
  }
}
