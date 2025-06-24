// Copyright (C) 2012 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.gerrit.server.restapi.project;

import com.google.common.flogger.FluentLogger;
import com.google.common.base.Strings;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.api.projects.PmsUrlInput;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.inject.Singleton;
import com.google.inject.Inject;
import java.io.IOException;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.MethodNotAllowedException;
import com.google.gerrit.server.project.ProjectConfig;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.errors.ConfigInvalidException;


@Singleton
public class PutPmsUrl implements RestModifyView<ProjectResource, PmsUrlInput> {
//below code commenting by Nikita jethava
  // @Override
  // public Response<String> apply(ProjectResource resource, PmsUrlInput input)
  //     throws AuthException, ResourceConflictException, ResourceNotFoundException {
  //   if (input == null) {
  //     input = new PmsUrlInput();
  //   }

  //   return Response.ok("added pms url");
  // }

  //New code added by Nikita jethava

  private final RepoMetaDataUpdater repoMetaDataUpdater;
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Inject
  PutPmsUrl(RepoMetaDataUpdater repoMetaDataUpdater) {
    this.repoMetaDataUpdater = repoMetaDataUpdater;
  }

  @Override
  public Response<String> apply(ProjectResource resource, PmsUrlInput input)
      throws AuthException,
          ResourceConflictException,
          ResourceNotFoundException,
          IOException,
          PermissionBackendException,
          BadRequestException,
          MethodNotAllowedException
          {

      logger.atInfo().log("Setting PMSUrl: %s", input.pmsUrl);
        if (input == null) {
          input = new PmsUrlInput();
      }

      try (var configUpdater =
          repoMetaDataUpdater.configUpdater(
              resource.getNameKey(), input.commitMessage, "Update pmsUrl")) {
        ProjectConfig config = configUpdater.getConfig();
        String pUrl = input.pmsUrl;
        config.updateProject(p -> p.setPmsUrl(Strings.emptyToNull(pUrl)));

        configUpdater.commitConfigUpdate();
        //Added By Nikita jethava for pms url
        configUpdater.getRepository().setGitwebPmsUrl(config.getProject().getPmsUrl());

        return Strings.isNullOrEmpty(config.getProject().getPmsUrl())
            ? Response.none()
            : Response.ok(config.getProject().getPmsUrl());
      } catch (RepositoryNotFoundException notFound) {
        throw new ResourceNotFoundException(resource.getName(), notFound);
      } catch (ConfigInvalidException e) {
        throw new ResourceConflictException(
            String.format("invalid project.config: %s", e.getMessage()));
      }
    }


}