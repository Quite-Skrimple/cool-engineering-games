package edu.arizona.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.arizona.catalog.model.entity.Game;
import edu.arizona.catalog.model.enums.ApprovalStatus;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByApprovalStatus(ApprovalStatus approvalStatus);

    List<Game> findByCreatorId(Long creatorId);
}
