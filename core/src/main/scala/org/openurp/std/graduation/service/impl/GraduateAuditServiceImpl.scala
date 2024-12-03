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

package org.openurp.std.graduation.service.impl

import org.beangle.commons.cdi.{Container, ContainerAware}
import org.beangle.commons.collection.Collections
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.beangle.ems.app.rule.RuleEngine
import org.openurp.base.std.model.{Graduate, Student}
import org.openurp.std.graduation.config.AuditSetting
import org.openurp.std.graduation.model.{GraduateBatch, GraduateResult}
import org.openurp.std.graduation.service.GraduateAuditService

import java.time.Instant

class GraduateAuditServiceImpl extends GraduateAuditService, ContainerAware {

  var entityDao: EntityDao = _
  var checkNames: String = "plan"
  var container: Container = _

  private def getSetting(std: Student): Option[AuditSetting] = {
    val project = std.project
    val q = OqlBuilder.from(classOf[AuditSetting], "setting")
    q.where("setting.project=:project", project)
    q.cacheable(true)
    val settings = entityDao.search(q)
    settings.find(x => x.levels.contains(std.level) && x.within(std.studyOn))
  }

  override def audit(result: GraduateResult): Unit = {
    result.passedItems = None
    result.failedItems = None

    val setting = getSetting(result.std).getOrElse(new AuditSetting)
    val engine = RuleEngine.get(setting.gruleIds)
    val results = engine.execute(result)
    results foreach { rs =>
      if (rs._2) {
        result.addPassed(rs._1.title, rs._3)
      } else {
        result.addFailed(rs._1.title, rs._3)
      }
    }
    result.updatedAt = Instant.now
    result.passed = Some(result.passedItems.nonEmpty & result.failedItems.isEmpty)
    //    result.passed foreach{passed=>
    //      if(passed){
    //        result.educationResult = Some(new EducationResult())
    //      }else{
    //
    //      }
    //    }
    entityDao.saveOrUpdate(result)
  }

  override def getResult(std: Student, batch: GraduateBatch): Option[GraduateResult] = {
    val query = OqlBuilder.from(classOf[GraduateResult], "result")
    query.where("result.std = :std and result.batch=:batch", std, batch)
    entityDao.search(query).headOption
  }

  override def initResult(std: Student, batch: GraduateBatch): GraduateResult = {
    val result = getResult(std, batch).getOrElse(new GraduateResult(std, batch))
    entityDao.saveOrUpdate(result)
    result
  }

  override def initResults(codes: collection.Seq[String], batch: GraduateBatch): Int = {
    val chunks = Collections.split(codes.toList, 500)
    var total = 0
    chunks foreach { chunk =>
      val query = OqlBuilder.from(classOf[Student], "std")
      query.where("std.code in(:codes)", chunk)
        .where("std.project = :project", batch.project)
      query.where(s"not exists (from ${classOf[GraduateResult].getName} gr where gr.std = std and gr.batch = :batch)", batch)
      val results = entityDao.search(query).map(new GraduateResult(_, batch))
      entityDao.saveOrUpdate(results)
      total += results.size
    }
    total
  }

  override def initResults(batch: GraduateBatch): Int = {
    val stdQuery = OqlBuilder.from(classOf[Student], "std")
    val graduateOn = batch.graduateOn
    stdQuery.where("std.project = :project", batch.project)
      // 学籍需要在正常毕业和学籍有效期内
      .where(":now between std.graduateOn and std.endOn", graduateOn)
      // 当期在校的
      .where("exists(from std.states as ss where :date between ss.beginOn and ss.endOn and ss.inschool=true)", graduateOn)
      // 本毕业季没有数据
      .where(s"not exists(from  ${classOf[GraduateResult].getName} gr where gr.std=std and gr.batch = :batch)", batch)
      // 也没有其他的已毕业数据
      .where(s"not exists(from  ${classOf[Graduate].getName} ga where ga.std=std and ga.graduateOn <> :dateOn)", graduateOn)
      // 其他毕业批次也没有通过的记录
      .where(s"not exists(from  ${classOf[GraduateResult].getName} ar where ar.std=std and ar.passed=true and ar.batch<>:batch)", batch)
    val results = entityDao.search(stdQuery).map(new GraduateResult(_, batch))
    entityDao.saveOrUpdate(results)
    results.size
  }
}
