package me.ancliz.minecraft.commands;

import com.mojang.brigadier.Command;

public record CommandSpec<S>(String root, String path, Class<?>[] args, Command<S> handler) {}