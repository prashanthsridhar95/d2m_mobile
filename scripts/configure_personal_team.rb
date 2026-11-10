require "xcodeproj"

project_path, team_id, bundle_id = ARGV
abort "usage: configure_personal_team.rb PROJECT TEAM_ID BUNDLE_ID" unless project_path && team_id && bundle_id

project = Xcodeproj::Project.open(project_path)
targets = {
  "iosApp" => bundle_id,
  "NotificationServiceExtension" => "#{bundle_id}.NotificationServiceExtension",
}

targets.each do |target_name, target_bundle_id|
  target = project.targets.find { |candidate| candidate.name == target_name }
  abort "target not found: #{target_name}" unless target

  configuration = target.build_configurations.find { |candidate| candidate.name == "Debug" }
  abort "Debug configuration not found: #{target_name}" unless configuration

  configuration.build_settings["DEVELOPMENT_TEAM"] = team_id
  configuration.build_settings["PRODUCT_BUNDLE_IDENTIFIER"] = target_bundle_id
  configuration.build_settings["CODE_SIGN_ENTITLEMENTS"] = "PersonalTeam.entitlements"
end

project.save