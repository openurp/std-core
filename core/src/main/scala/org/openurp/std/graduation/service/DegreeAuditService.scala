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

package org.openurp.std.graduation.service

import org.openurp.base.std.model.Student
import org.openurp.std.graduation.model.{DegreeResult, GraduateBatch, GraduateResult}

trait DegreeAuditService {
  def audit(result: DegreeResult): Unit

  def getResult(std: Student, batch: GraduateBatch): Option[DegreeResult]

  def initResult(std: Student, batch: GraduateBatch): DegreeResult

  def initResults(codes: collection.Seq[String], batch: GraduateBatch): Int

  def initResults(batch: GraduateBatch): Int

  def publish(results: Iterable[DegreeResult], published: Boolean): Unit
}
