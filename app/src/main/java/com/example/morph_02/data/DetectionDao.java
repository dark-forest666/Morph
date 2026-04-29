package com.example.morph_02.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DetectionDao {
    // 插入单条记录
    @Insert
    long insert(DetectionEntity entity);

    // 更新记录（如修改reminded状态）
    @Update
    int update(DetectionEntity entity);

    // 查询所有记录（按时间倒序）
    @Query("SELECT * FROM detection_records ORDER BY timestamp DESC")
    List<DetectionEntity> getAllRecords();

    // 删除所有记录
    @Query("DELETE FROM detection_records")
    void deleteAll();

    // 根据ID查询单条记录
    @Query("SELECT * FROM detection_records WHERE id = :id")
    DetectionEntity getRecordById(long id);
}