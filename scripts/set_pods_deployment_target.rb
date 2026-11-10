require "xcodeproj"

project_path, deployment_target = ARGV
abort "usage: set_pods_deployment_target.rb PROJECT TARGET" unless project_path && deployment_target

project = Xcodeproj::Project.open(project_path)
changed = false

project.targets.each do |target|
  target.build_configurations.each do |configuration|
    next if configuration.build_settings["IPHONEOS_DEPLOYMENT_TARGET"] == deployment_target

    configuration.build_settings["IPHONEOS_DEPLOYMENT_TARGET"] = deployment_target
    changed = true
  end
end

project.save if changed